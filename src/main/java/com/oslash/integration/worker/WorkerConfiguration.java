package com.oslash.integration.worker;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oslash.integration.config.AppConfiguration;
import com.oslash.integration.manager.ManagerConfiguration;
import com.oslash.integration.models.FileMeta;
import com.oslash.integration.worker.writer.FileMetaWriter;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.aws.inbound.SqsMessageDrivenChannelAdapter;
import org.springframework.integration.aws.outbound.SqsMessageHandler;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.integration.transformer.Transformer;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Profile("worker")
public class WorkerConfiguration {

    @Autowired
    AppConfiguration appConfiguration;

    @Autowired
    FileMetaWriter fileMetaWriter;

    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    
    @Autowired
    private RemotePartitioningWorkerStepBuilderFactory stepBuilderFactory;

    @Autowired
    private DataSource dataSource;

    @Bean
    @ConditionalOnMissingBean(value = ObjectNamingStrategy.class, search = SearchStrategy.CURRENT)
    public IntegrationFlow inboundFlow(@Qualifier("amazonSQSRequestAsync") AmazonSQSAsync sqsAsync) {
        SqsMessageDrivenChannelAdapter adapter = new SqsMessageDrivenChannelAdapter(sqsAsync, appConfiguration.getRequestQueName());
        return IntegrationFlows.from(adapter)
                .transform(messageTransformer())
                .channel(requests())
                .get();
    }

    @Bean
    public IntegrationFlow outboundFlow(@Qualifier("amazonSQSReplyAsync") AmazonSQSAsync sqsAsync) {
        SqsMessageHandler sqsMessageHandler = new SqsMessageHandler(sqsAsync);
        sqsMessageHandler.setQueue(appConfiguration.getReplyQueName());
        return IntegrationFlows.from(replies())
                .transform(objectToJsonTransformer())
                .log()
                .handle(sqsMessageHandler)
                .get();
    }

    /**
     *
     * issue: https://github.com/spring-projects/spring-batch/issues/1488#issuecomment-566278703
     * org.springframework.integration.json.ObjectToJsonTransformer#jsonObjectMapper
     * @return
     */
    @Bean
    public Jackson2JsonObjectMapper jacksonJsonBuilder() {
        Jackson2JsonObjectMapper b = new Jackson2JsonObjectMapper();
        ObjectMapper mapper = b.getObjectMapper();
        Map<Class<?>, Class<?>> mixIns = new LinkedHashMap<>();
        mixIns.put(org.springframework.batch.core.StepExecution.class, ManagerConfiguration.StepExecutionsMixin.class);
        mixIns.put(org.springframework.batch.core.JobExecution.class, ManagerConfiguration.JobExecutionMixin.class);
        mixIns.forEach(mapper::addMixIn);
        return b;
    }

    @Bean
    public ObjectToJsonTransformer objectToJsonTransformer() {
        return new ObjectToJsonTransformer(jacksonJsonBuilder());
    }

    @Bean
    public Transformer messageTransformer() {
        return new MessageTransformer();
    }

    @Bean
    public DirectChannel requests() {
        return new DirectChannel();
    }

    @Bean
    public DirectChannel replies() {
        return new DirectChannel();
    }

    @Bean(name = "simpleStep")
    public Step simpleStep() {
        return stepBuilderFactory.get(appConfiguration.getStepName())
                .inputChannel(requests())
                .<Map, FileMeta>chunk(100)
                .reader(itemReader(null))
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }

    @Bean
    public ItemWriter<FileMeta> itemWriter() {
        return fileMetaWriter;
    }

    @Bean
    public ItemProcessor<Map, FileMeta> itemProcessor() {
        return new ItemProcessor<>() {
            @Override
            public FileMeta process(Map item) {
                return new FileMeta.Builder().file(item).build();
            }
        };
    }

    @Bean
    @StepScope
    public ItemReader<Map> itemReader(@Value("#{stepExecutionContext['data']}") List<Map> data) {
        List<Map> remainingData = new ArrayList<>(data);
        return new ItemReader<>() {
            @Override
            public Map read() {
                if (remainingData.size() > 0) {
                    return remainingData.remove(0);
                }
                return null;
            }
        };
    }
}
