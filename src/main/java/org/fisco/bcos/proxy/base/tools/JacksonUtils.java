/**
 * Copyright 2014-2020 the original author or authors.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fisco.bcos.proxy.base.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class JacksonUtils {
    // data format
    private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";
    public static final ThreadLocal<ObjectMapper> OBJECT_MAPPER =
            new ThreadLocal<ObjectMapper>() {
                @Override
                protected ObjectMapper initialValue() {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
                    JavaTimeModule javaTimeModule = new JavaTimeModule();
                    javaTimeModule.addSerializer(
                            LocalDateTime.class,
                            new LocalDateTimeSerializer(
                                    DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)));
                    javaTimeModule.addSerializer(
                            LocalDate.class,
                            new LocalDateSerializer(
                                    DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
                    javaTimeModule.addSerializer(
                            LocalTime.class,
                            new LocalTimeSerializer(
                                    DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)));
                    javaTimeModule.addDeserializer(
                            LocalDateTime.class,
                            new LocalDateTimeDeserializer(
                                    DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)));
                    javaTimeModule.addDeserializer(
                            LocalDate.class,
                            new LocalDateDeserializer(
                                    DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
                    javaTimeModule.addDeserializer(
                            LocalTime.class,
                            new LocalTimeDeserializer(
                                    DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)));
                    objectMapper
                            .registerModule(javaTimeModule)
                            .registerModule(new ParameterNamesModule())
                            .registerModule(new Jdk8Module());
                    return objectMapper;
                }
            };

    public static boolean isJson(String str) {
        try {
            OBJECT_MAPPER.get().readTree(str);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static JsonNode stringToJsonNode(String str) {
        try {
            return OBJECT_MAPPER.get().readTree(str);
        } catch (IOException e) {
            log.error("Parse String to JsonNode error : {}", e.getMessage());
            return null;
        }
    }

    public static <T> String objToString(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return obj instanceof String
                    ? (String) obj
                    : OBJECT_MAPPER.get().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Parse Object to String error : {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T stringToObj(String str, Class<T> clazz) {
        if (StringUtils.isEmpty(str) || clazz == null) {
            return null;
        }
        try {
            return clazz.equals(String.class) ? (T) str : OBJECT_MAPPER.get().readValue(str, clazz);
        } catch (Exception e) {
            log.error("Parse String to Object error : {}", e.getMessage());
            return null;
        }
    }
}
