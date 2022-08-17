/*
 * DISCLAIMER
 *
 * Copyright 2017 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.springframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.Persistent;

import com.arangodb.entity.KeyType;

/**
 * Annotation to identify a domain object to be persisted into a ArangoDB edge
 * collection.
 *
 * @author Mark Vollmary
 *
 */
@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Edge {

	/**
	 * Alias for {@link #collection}.
	 * <p>
	 * Intended to be used instead of {@link #collection} when no other attributes
	 * are needed &mdash; for example: {@code @Document("collection")} instead of
	 * {@code @Document(collection = "collection")}.
	 */
	@AliasFor("collection")
	String value() default "";

	/**
	 * The name of the edge collection
	 */
	@AliasFor("value")
	String collection() default "";

	/**
	 * (The default is 1): in a cluster, this attribute determines how many copies
	 * of each shard are kept on different DBServers. The value 1 means that only
	 * one copy (no synchronous replication) is kept. A value of k means that k-1
	 * replicas are kept. Any two copies reside on different DBServers. Replication
	 * between them is synchronous, that is, every write operation to the "leader"
	 * copy will be replicated to all "follower" replicas, before the write
	 * operation is reported successful. If a server fails, this is detected
	 * automatically and one of the servers holding copies takes over, usually
	 * without an error being reported.
	 */
	int replicationFactor() default -1;

	/**
	 * If true the collection is created as a satellite collection. In this case
	 * {@link #replicationFactor()} is ignored.
	 */
	boolean satellite() default false;

	/**
	 * If true then the data is synchronized to disk before returning from a
	 * document create, update, replace or removal operation. (default: false)
	 */
	boolean waitForSync() default false;


	/**
	 * (The default is [ "_key" ]): in a cluster, this attribute determines which
	 * document attributes are used to determine the target shard for documents.
	 * Documents are sent to shards based on the values of their shard key
	 * attributes. The values of all shard key attributes in a document are hashed,
	 * and the hash value is used to determine the target shard. Note: Values of
	 * shard key attributes cannot be changed once set. This option is meaningless
	 * in a single server setup.
	 */
	String[] shardKeys() default {};

	/**
	 * (The default is 1): in a cluster, this value determines the number of shards
	 * to create for the collection. In a single server setup, this option is
	 * meaningless.
	 */
	int numberOfShards() default -1;

	/**
	 * If true, create a system collection. In this case collection-name should
	 * start with an underscore. End users should normally create non-system
	 * collections only. API implementors may be required to create system
	 * collections in very special occasions, but normally a regular collection will
	 * do. (The default is false)
	 */
	boolean isSystem() default false;

	/**
	 * If set to true, then it is allowed to supply own key values in the _key
	 * attribute of a document. If set to false, then the key generator will solely
	 * be responsible for generating keys and supplying own key values in the _key
	 * attribute of documents is considered an error.
	 */
	boolean allowUserKeys() default false;

	/**
	 * Specifies the type of the key generator. The currently available generators
	 * are traditional and autoincrement.
	 */
	KeyType keyType() default KeyType.traditional;

	/**
	 * Increment value for autoincrement key generator. Not used for other key
	 * generator types.
	 */
	int keyIncrement() default -1;

	/**
	 * Initial offset value for autoincrement key generator. Not used for other key
	 * generator types.
	 */
	int keyOffset() default -1;

}
