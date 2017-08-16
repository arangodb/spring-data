package com.arangodb.springframework.core.repository.cdi;

import com.arangodb.springframework.core.ArangoOperations;
import org.springframework.data.repository.cdi.CdiRepositoryBean;
import org.springframework.data.repository.cdi.CdiRepositoryExtensionSupport;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.ProcessBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by F625633 on 13/07/2017.
 */
public class ArangoRepositoryExtension extends CdiRepositoryExtensionSupport {

	private final Map<Set<Annotation>, Bean<ArangoOperations>> operations = new HashMap<>();

	public ArangoRepositoryExtension() {}

	<X> void processBean(@Observes ProcessBean<X> processBean) {
		Bean<X> bean = processBean.getBean();
		for (Type type : bean.getTypes()) {
			if (type instanceof Class<?> && ArangoOperations.class.isAssignableFrom((Class<?>) type)) {
				operations.put(new HashSet<Annotation>(bean.getQualifiers()), (Bean<ArangoOperations>) bean);
			}
		}
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
		for (Map.Entry<Class<?>, Set<Annotation>> entry : getRepositoryTypes()) {
			Class<?> repositoryType = entry.getKey();
			Set<Annotation> qualifiers = entry.getValue();
			CdiRepositoryBean<?> repositoryBean = createRepositoryBean(repositoryType, qualifiers, beanManager);
			registerBean(repositoryBean);
			afterBeanDiscovery.addBean(repositoryBean);
		}
	}

	private <T> CdiRepositoryBean<T> createRepositoryBean(
			Class<T> repositoryType, Set<Annotation> qualifiers, BeanManager beanManager
	) {
		Bean<ArangoOperations> operations = this.operations.get(qualifiers);
		if (operations == null) throw new UnsatisfiedResolutionException("Unable to resolve ArangoOperations for CDI");
		return new ArangoRepositoryBean<T>(
				operations, qualifiers, repositoryType, beanManager, getCustomImplementationDetector()
		);
	}
}
