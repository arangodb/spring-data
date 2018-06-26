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

import org.springframework.data.annotation.Persistent;

import com.arangodb.entity.KeyType;

/**
 * @author Mark Vollmary
 *
 */
@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Edge {

	/**
	 * @return The name of the collection
	 */
	String value() default "";

	/**
	 * @return The maximal size of a journal or datafile in bytes. The value must be at least 1048576 (1 MiB).
	 */
	long journalSize() default -1;

	/**
	 * @return (The default is 1): in a cluster, this attribute determines how many copies of each shard are kept on
	 *         different DBServers. The value 1 means that only one copy (no synchronous replication) is kept. A value
	 *         of k means that k-1 replicas are kept. Any two copies reside on different DBServers. Replication between
	 *         them is synchronous, that is, every write operation to the "leader" copy will be replicated to all
	 *         "follower" replicas, before the write operation is reported successful. If a server fails, this is
	 *         detected automatically and one of the servers holding copies takes over, usually without an error being
	 *         reported.
	 */
	int replicationFactor() default -1;

	/**
	 * @return If true the collection is created as a satellite collection. In this case {@link #replicationFactor()} is
	 *         ignored.
	 */
	boolean satellite() default false;

	/**
	 * @return If true then the data is synchronized to disk before returning from a document create, update, replace or
	 *         removal operation. (default: false)
	 */
	boolean waitForSync() default false;

	/**
	 * @return whether or not the collection will be compacted (default is true)
	 */
	boolean doCompact() default true;

	/**
	 * @return If true then the collection data is kept in-memory only and not made persistent. Unloading the collection
	 *         will cause the collection data to be discarded. Stopping or re-starting the server will also cause full
	 *         loss of data in the collection. Setting this option will make the resulting collection be slightly faster
	 *         than regular collections because ArangoDB does not enforce any synchronization to disk and does not
	 *         calculate any CRC checksums for datafiles (as there are no datafiles). This option should therefore be
	 *         used for cache-type collections only, and not for data that cannot be re-created otherwise. (The default
	 *         is false)
	 */
	boolean isVolatile() default false;

	/**
	 * @return (The default is [ "_key" ]): in a cluster, this attribute determines which document attributes are used
	 *         to determine the target shard for documents. Documents are sent to shards based on the values of their
	 *         shard key attributes. The values of all shard key attributes in a document are hashed, and the hash value
	 *         is used to determine the target shard. Note: Values of shard key attributes cannot be changed once set.
	 *         This option is meaningless in a single server setup.
	 */
	String[] shardKeys() default {};

	/**
	 * @return (The default is 1): in a cluster, this value determines the number of shards to create for the
	 *         collection. In a single server setup, this option is meaningless.
	 */
	int numberOfShards() default -1;

	/**
	 * @return If true, create a system collection. In this case collection-name should start with an underscore. End
	 *         users should normally create non-system collections only. API implementors may be required to create
	 *         system collections in very special occasions, but normally a regular collection will do. (The default is
	 *         false)
	 */
	boolean isSystem() default false;

	/**
	 * @return The number of buckets into which indexes using a hash table are split. The default is 16 and this number
	 *         has to be a power of 2 and less than or equal to 1024. For very large collections one should increase
	 *         this to avoid long pauses when the hash table has to be initially built or resized, since buckets are
	 *         resized individually and can be initially built in parallel. For example, 64 might be a sensible value
	 *         for a collection with 100 000 000 documents. Currently, only the edge index respects this value, but
	 *         other index types might follow in future ArangoDB versions. Changes (see below) are applied when the
	 *         collection is loaded the next time.
	 */
	int indexBuckets() default -1;

	/**
	 * @return if set to true, then it is allowed to supply own key values in the _key attribute of a document. If set
	 *         to false, then the key generator will solely be responsible for generating keys and supplying own key
	 *         values in the _key attribute of documents is considered an error.
	 */
	boolean allowUserKeys() default false;

	/**
	 * @return specifies the type of the key generator. The currently available generators are traditional and
	 *         autoincrement.
	 */
	KeyType keyType() default KeyType.traditional;

	/**
	 * @return increment value for autoincrement key generator. Not used for other key generator types.
	 */
	int keyIncrement() default -1;

	/**
	 * @return Initial offset value for autoincrement key generator. Not used for other key generator types.
	 */
	int keyOffset() default -1;

}
