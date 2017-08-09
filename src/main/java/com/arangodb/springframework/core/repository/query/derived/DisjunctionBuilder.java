package com.arangodb.springframework.core.repository.query.derived;

import java.util.LinkedList;

/**
 * Created by F625633 on 24/07/2017.
 */
public class DisjunctionBuilder {

    private static final String ARRAY_DELIMITER = ", ";
    private static final String PREDICATE_DELIMITER = " OR ";
    private static final String SUBQUERY_TEMPLATE = "(FOR e in %s FILTER %s RETURN %s)";

    private final DerivedQueryCreator queryCreator;

    private final LinkedList<Conjunction> conjunctions = new LinkedList<>();

    private final StringBuilder arrayStringBuilder = new StringBuilder();
    private final StringBuilder predicateStringBuilder = new StringBuilder();

    private int arrays = 0;

    public DisjunctionBuilder(DerivedQueryCreator queryCreator) {
        this.queryCreator = queryCreator;
    }

    public void add(Conjunction conjunction) {
        conjunctions.add(conjunction);
        if (conjunction.isArray()) {
            ++arrays;
            String array = conjunction.hasPredicate() ?
                    String.format(SUBQUERY_TEMPLATE, conjunction.getArray(), conjunction.getPredicate(), "e")
                    : conjunction.getArray();
            arrayStringBuilder.append((arrayStringBuilder.length() == 0 ? "" : ARRAY_DELIMITER) + array);
        } else {
            predicateStringBuilder.append(
                    (predicateStringBuilder.length() == 0 ? "" : PREDICATE_DELIMITER) + conjunction.getPredicate());
        }
    }

    private String buildArrayString() {
        if (conjunctions.size() == 1 && conjunctions.get(0).isComposite())
            return conjunctions.get(0).getArray();
        boolean shouldPredicateBeBuilt = arrayStringBuilder.length() != 0 && predicateStringBuilder.length() != 0;
        if (shouldPredicateBeBuilt) {
            String distanceAdjusted = "e";
            if (!queryCreator.getGeoFields().isEmpty()) {
                String geoFields = queryCreator.getGeoFields().size() == 1 ?
                        String.format("e.%s[0], e.%s[1]", queryCreator.getGeoFields().get(0), queryCreator.getGeoFields().get(0))
                        : String.format("e.%s, e.%s", queryCreator.getGeoFields().get(0), queryCreator.getGeoFields().get(1));
                distanceAdjusted = String.format("MERGE(e, {'_distance': DISTANCE(%s, %%f, %%f)})", geoFields);
            }
            String array = String.format(SUBQUERY_TEMPLATE, queryCreator.getCollectionName(), predicateStringBuilder.toString(), distanceAdjusted);
            arrayStringBuilder.append((arrayStringBuilder.length() == 0 ? "" : ARRAY_DELIMITER) + array);
        }
        if (arrays > 1 || shouldPredicateBeBuilt)
            return "UNION(" + arrayStringBuilder.toString() + ")";
        return arrayStringBuilder.toString();
    }

    private String buildPredicateSring() {
        if (conjunctions.size() == 1 && conjunctions.get(0).isComposite())
            return conjunctions.get(0).getPredicate();
        return arrayStringBuilder.length() == 0 ? predicateStringBuilder.toString() : "";
    }

    public Disjunction build() {
        String arrayString = String.format(buildArrayString(), queryCreator.getUniquePoint()[0], queryCreator.getUniquePoint()[1]);
        return new Disjunction(arrayString, buildPredicateSring());
    }
}
