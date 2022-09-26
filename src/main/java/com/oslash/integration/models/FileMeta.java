package com.oslash.integration.models;

import com.oslash.integration.utils.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

import static java.util.Objects.nonNull;


/**
 * The type File meta.
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "file_meta")
public class FileMeta {
    @Id
    private String id;
    private String mimeType;

    private String userId;
    private Object content;

    /**
     * Instantiates a new File meta.
     *
     * @param id       the id
     * @param mimeType the mime type
     * @param content  the content
     */
    public FileMeta(String id, String mimeType, Object content) {
        this.id = id;
        this.mimeType = mimeType;
        this.content = content;
    }

    /**
     * Instantiates a new File meta.
     *
     * @param builder the builder
     */
    public FileMeta(Builder builder) {
        setId(builder.id);
        setUserId(builder.userId);
        setMimeType(builder.mimeType);
        setContent(builder.content);
    }

    /**
     * The type Builder.
     */
    public static final class Builder {
        private String id;
        private String mimeType;
        private Object content;

        private String userId;

        /**
         * Instantiates a new Builder.
         */
        public Builder() {
        }

        /**
         * Id builder.
         *
         * @param val the val
         * @return the builder
         */
        public Builder id(String val) {
            id = val;
            return this;
        }

        /**
         * Mime type builder.
         *
         * @param val the val
         * @return the builder
         */
        public Builder mimeType(String val) {
            mimeType = val;
            return this;
        }

        /**
         * User id builder.
         *
         * @param val the val
         * @return the builder
         */
        public Builder userId(String val) {
            userId = val;
            return this;
        }

        /**
         * Content builder.
         *
         * @param val the val
         * @return the builder
         */
        public Builder content(Object val) {
            content = val;
            return this;
        }

        /**
         * File builder.
         *
         * @param item the item
         * @return the builder
         */
        public Builder file(Map item) {
            this.content = item;
            if (nonNull(item.get(Constants.FILE_ID))) {
                this.id = String.valueOf(item.get(Constants.FILE_ID));
            }
            if (nonNull(item.get(Constants.MIME_TYPE))) {
                this.mimeType = String.valueOf(item.get(Constants.MIME_TYPE));
            }
            if (nonNull(item.get(Constants.USER_ID))) {
                this.userId = String.valueOf(item.get(Constants.USER_ID));
            }
            if (nonNull(item.get(Constants.CONTENT))) {
                this.content = String.valueOf(item.get(Constants.CONTENT));
            }
            return this;
        }

        /**
         * Build file meta.
         *
         * @return the file meta
         */
        public FileMeta build() {
            return new FileMeta(this);
        }
    }
}