package com.arangodb.springframework.core.template;

class CollectionCacheKey {

	private final String db;
	private final String collection;

	public CollectionCacheKey(final String db, final String collection) {
		super();
		this.db = db;
		this.collection = collection;
	}

	public String getDb() {
		return db;
	}

	public String getCollection() {
		return collection;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((collection == null) ? 0 : collection.hashCode());
		result = prime * result + ((db == null) ? 0 : db.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final CollectionCacheKey other = (CollectionCacheKey) obj;
		if (collection == null) {
			if (other.collection != null) {
				return false;
			}
		} else if (!collection.equals(other.collection)) {
			return false;
		}
		if (db == null) {
			if (other.db != null) {
				return false;
			}
		} else if (!db.equals(other.db)) {
			return false;
		}
		return true;
	}

}