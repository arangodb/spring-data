package com.arangodb.springframework.core.repository.cdi;

import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.repository.ArangoRepositoryFactory;
import org.springframework.data.repository.cdi.CdiRepositoryBean;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

/**
 * Created by F625633 on 13/07/2017.
 */
public class ArangoRepositoryBean<T> extends CdiRepositoryBean<T> {

	private final Bean<ArangoOperations> operations;

	public ArangoRepositoryBean(
			Bean<ArangoOperations> operations,
			Set<Annotation> qualifiers,
			Class<T> repositoryType,
			BeanManager beanManager,
			CustomRepositoryImplementationDetector detector
	) {
		super(qualifiers, repositoryType, beanManager, detector);
		this.operations = operations;
	}

	@Override
	protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType, Object customImplementation) {
		ArangoOperations operations = getDependencyInstance(this.operations, ArangoOperations.class);
		ArangoRepositoryFactory factory = new ArangoRepositoryFactory(operations);
		return factory.getRepository(repositoryType, customImplementation);
	}
}
