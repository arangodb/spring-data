package com.arangodb.springframework.core.repository.query;

import com.arangodb.model.AqlQueryOptions;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The main class used to access parameters of a user-defined method.
 * It wraps ParametersParameterAccessor which catches special parameters Sort and Pageable,
 * and catches Arango-specific parameters e.g. AqlQueryOptions
 */
public class ArangoParameterAccessor implements ParameterAccessor {
    private final ParametersParameterAccessor accessor;
    private final List<Object> bindableArguments;
    private AqlQueryOptions options = null;

    public ArangoParameterAccessor(ArangoParameters parameters, Object[] arguments) {
        accessor = new ParametersParameterAccessor(parameters, arguments);
        this.bindableArguments = createBindableArguments(arguments);
    }

    AqlQueryOptions getAqlQueryOptions() { return options; }

    @Override
    public Pageable getPageable() { return accessor.getPageable(); }

    @Override
    public Sort getSort() { return accessor.getSort(); }

    @Override
    public Class<?> getDynamicProjection() { return accessor.getDynamicProjection(); }

    @Override
    public Object getBindableValue(int index) { return accessor.getBindableValue(index); }

    @Override
    public boolean hasBindableNullValue() { return accessor.hasBindableNullValue(); }

    @Override
    public Iterator<Object> iterator() { return bindableArguments.iterator(); }

    private List<Object> createBindableArguments(Object[] arguments) {
        List<Object> bindableArguments = new LinkedList<>();
        for (Parameter parameter : accessor.getParameters().getBindableParameters()) {
            if (parameter.getType() == AqlQueryOptions.class) {
                Assert.isTrue(options == null, "AqlQueryOptions duplicated");
                options = (AqlQueryOptions) arguments[parameter.getIndex()];
            } else {
                bindableArguments.add(arguments[parameter.getIndex()]);
            }
        }
        return bindableArguments;
    }
}
