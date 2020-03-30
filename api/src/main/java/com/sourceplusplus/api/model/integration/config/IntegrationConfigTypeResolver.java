package com.sourceplusplus.api.model.integration.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.4
 * @since 0.2.0
 */
public class IntegrationConfigTypeResolver extends TypeIdResolverBase {

    private JavaType superType;

    @Override
    public void init(JavaType baseType) {
        superType = baseType;
    }

    @Override
    public String idFromValue(Object value) {
        if (value instanceof ApacheSkyWalkingIntegrationConfig) {
            return "apache_skywalking";
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.NAME;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        Class<?> subType = IntegrationConfig.class;
        switch (id) {
            case "apache_skywalking":
                subType = ApacheSkyWalkingIntegrationConfig.class;
                break;
        }
        return context.constructSpecializedType(superType, subType);
    }
}