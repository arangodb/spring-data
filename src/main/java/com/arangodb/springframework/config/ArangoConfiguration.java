/**
 *
 */
package com.arangodb.springframework.config;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.arangodb.ContentType;
import com.arangodb.serde.ArangoSerde;
import com.arangodb.serde.jackson.JacksonMapperProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;

import com.arangodb.ArangoDB;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.ArangoCustomConversions;
import com.arangodb.springframework.core.convert.ArangoTypeMapper;
import com.arangodb.springframework.core.convert.DefaultArangoConverter;
import com.arangodb.springframework.core.convert.DefaultArangoTypeMapper;
import com.arangodb.springframework.core.convert.resolver.DefaultResolverFactory;
import com.arangodb.springframework.core.convert.resolver.ResolverFactory;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.template.ArangoTemplate;

/**
 * Defines methods to customize the Java-based configuration for Spring Data
 * ArangoDB.
 *
 * @author Mark Vollmary
 */
public interface ArangoConfiguration {

    ArangoDB.Builder arango();

    String database();

    /**
     * Configures the behaviour of {@link com.arangodb.springframework.repository.ArangoRepository#save(Object)} and
     * {@link com.arangodb.springframework.repository.ArangoRepository#saveAll(Iterable)} to either return the original
     * entities (updated where possible) or new ones.
     * Set to {@code false} to use immutable entity classes or java records.
     */
    default boolean returnOriginalEntities() {
        return true;
    }

    /**
     * Override to set the data format to use in {@link #serde()}. It must match the content-type required by the
     * protocol used in the driver, e.g. set to {@link ContentType#VPACK} for protocols
     * {@link com.arangodb.Protocol#VST}, {@link com.arangodb.Protocol#HTTP_VPACK} and
     * {@link com.arangodb.Protocol#HTTP2_VPACK}, or set to {@link ContentType#VPACK} otherwise.
     *
     * @return the content-type to use in {@link #serde()}
     */
    default ContentType contentType() {
        return ContentType.JSON;
    }

    @Bean
    default ArangoTemplate arangoTemplate() throws Exception {
        return new ArangoTemplate(arango().serde(serde()).build(), database(), arangoConverter(), resolverFactory());
    }

    @Bean
    default ArangoSerde serde() throws Exception {
        return new ArangoSerde() {
            private final ObjectMapper om = JacksonMapperProvider.of(contentType());
            private final ArangoConverter converter = arangoConverter();

            @Override
            public byte[] serialize(Object value) {
                try {
                    return om.writeValueAsBytes(converter.write(value));
                } catch (JsonProcessingException e) {
                    throw new MappingException("Exception while serializing.", e);
                }
            }

            @Override
            public <T> T deserialize(byte[] content, Class<T> clazz) {
                try {
                    return converter.read(clazz, om.readTree(content));
                } catch (IOException e) {
                    throw new MappingException("Exception while deserializing.", e);
                }
            }
        };
    }

    @Bean
    default ArangoMappingContext arangoMappingContext() throws Exception {
        final ArangoMappingContext context = new ArangoMappingContext();
        context.setInitialEntitySet(getInitialEntitySet());
        context.setFieldNamingStrategy(fieldNamingStrategy());
        context.setSimpleTypeHolder(customConversions().getSimpleTypeHolder());
        return context;
    }

    @Bean
    default ArangoConverter arangoConverter() throws Exception {
        return new DefaultArangoConverter(arangoMappingContext(), customConversions(), resolverFactory(),
                arangoTypeMapper());
    }

    default CustomConversions customConversions() {
        return new ArangoCustomConversions(customConverters());
    }

    default Collection<Converter<?, ?>> customConverters() {
        return Collections.emptyList();
    }

    default Set<? extends Class<?>> getInitialEntitySet() throws ClassNotFoundException {
        return ArangoEntityClassScanner.scanForEntities(getEntityBasePackages());
    }

    default String[] getEntityBasePackages() {
        return new String[]{getClass().getPackage().getName()};
    }

    default FieldNamingStrategy fieldNamingStrategy() {
        return PropertyNameFieldNamingStrategy.INSTANCE;
    }

    default String typeKey() {
        return DefaultArangoTypeMapper.DEFAULT_TYPE_KEY;
    }

    default ArangoTypeMapper arangoTypeMapper() throws Exception {
        return new DefaultArangoTypeMapper(typeKey(), arangoMappingContext());
    }

	@Bean
    default ResolverFactory resolverFactory() {
		return new DefaultResolverFactory();
    }
}