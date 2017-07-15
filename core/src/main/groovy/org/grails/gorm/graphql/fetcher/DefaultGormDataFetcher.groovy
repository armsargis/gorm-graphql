package org.grails.gorm.graphql.fetcher

import grails.gorm.DetachedCriteria
import graphql.language.Field
import graphql.language.Selection
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty

/**
 * A generic class to assist with querying entities with GraphQL
 *
 * @param <T> The domain type to query
 * @author James Kleeh
 */
@CompileStatic
abstract class DefaultGormDataFetcher<T> implements DataFetcher<T> {

    protected List<String> associationNames
    protected PersistentEntity entity

    DefaultGormDataFetcher(PersistentEntity entity) {
        this.entity = entity
        this.associationNames = entity.associations*.name
    }

    protected Map defaultQueryOptions(DataFetchingEnvironment environment) {
        Set<String> joinProperties = []

        environment.fields.each { field ->
            field.selectionSet.selections.each { Selection selection ->
                if (selection instanceof Field) {
                    final String name = ((Field)selection).name
                    if (associationNames.contains(name)) {
                        joinProperties.add(name)
                    }
                }
            }
        }

        if (joinProperties) {
            [fetch: joinProperties.collectEntries { [(it): "join"] }]
        }
        else {
            [:]
        }
    }

    protected GormEntity queryInstance(DataFetchingEnvironment environment) {
        List<String> idNames = []

        if (entity.identity != null) {
            idNames.add(entity.identity.name)
        }
        else if (entity.compositeIdentity != null) {
            entity.compositeIdentity.each { PersistentProperty p ->
                idNames.add(p.name)
            }
        }

        (GormEntity)new DetachedCriteria(entity.javaClass).build {
            idNames.each {
                eq(it, environment.getArgument(it))
            }
        }.get(defaultQueryOptions(environment))
    }

    abstract T get(DataFetchingEnvironment environment)
}