package com.app.risk.config;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphQLConfig implements RuntimeWiringConfigurer {

    @Override
    public void configure(RuntimeWiring.Builder builder) {
        GraphQLScalarType longScalar = GraphQLScalarType.newScalar()
                .name("Long")
                .coercing(new Coercing<Long, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) {
                        return dataFetcherResult.toString();
                    }

                    @Override
                    public Long parseValue(Object input) {
                        return Long.parseLong(input.toString());
                    }

                    @Override
                    public Long parseLiteral(Object input) {
                        if (input instanceof StringValue) {
                            return Long.parseLong(((StringValue) input).getValue());
                        }
                        return null;
                    }
                })
                .build();
        builder.scalar(longScalar);
    }
}
