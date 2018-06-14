![ArangoDB-Logo](https://docs.arangodb.com/assets/arangodb_logo_2016_inverted.png)

# ArangoDB Spring Data (Supporting Canonical COLLECTION-PER-CLASS Type of Inheritance) Rational(ly) 

One maintainer & 1 contributor in Spring Data ArangoDB project have refused to accept inheritance-related contributions implemented here. That decision has obviously
(& without doubt) been driven not by rational considerations about technology, but by something else. In the process of blocking the contributions implemented here 
Spring Data ArangoDB upstream project has become tainted by extremely severe inefficiencies & irrationality. The developer who has provided the inheritance-related
contributions implemented here, believes that what is now in the upstream is so irrational that it cannot be used as is, & therefore has to use a fork that 
provides rational & efficient implementation for a main-stream persistence-related inheritance type like canonical COLLECTION-PER-CLASS approach (similar to 
TABLE-PER-CLASS inheritance type in JPA). The expression canonical COLLECTION-PER-CLASS type of inheritance is used here not as something set in stone, but just to avoid 
using a more ambiguous phrase like "classes that have a declared @Document annotation". **Bottom line is that this implementation is now more efficient than upstream, 
even for projects that don't use any persistence-related inheritance at all**, because the upstream project has become inefficient & irrational for all 
records (whether or not any persistence-related inheritance is involved in them). 

* [Inefficiencies & other issues in Spring Data ArangoDB optimized by this implementation](#inefficiencies_optimized)
    * [Visual examples of optimized inefficiencies](#visuals)
       * [Single record](#single)
       * [A record for a class that DOESN'T extend another entity/document, & is not extended](#noinheritance)
       * [A record for a class that has a property of type List with 2 entities/documents in it](#list)
    * [Cumulative effect of optimizations (for JOINs, multiple records matching a query, etc.)](#multiples)
    * [Cumulative efficiencies: simple sample calculations for various numbers of persisted entities](#calc)
* [Test report comparisons (showing that all upstream functionality is preserved, it is just optimized (not less, just better))](#testing)
* [Brief history](#history)

## <a name="inefficiencies_optimized"></a>Inefficiencies & other issues in Spring Data ArangoDB OPTIMIZED/RESOLVED by this implementation
1. Data pollution & disk space waste: amount of data persisted/processed, etc. when using this implementation is [up to 4 times smaller](#single).
2. This data pollution & disk space waste in turn entail more memory utilization at run-time.
3. This also entails unnecessary band-width utilization.
4. All of the above also entail usage of more CPU cycles at run-time (considering storage of the unnecessary data, its retrieval, & processing).
5. Issues 1-through-4, can lead to considerable & even noticeable increase in latency (responsiveness). 
6. Issues 1-through-4, (especially when using a Platform as a service) eventually (for a PaaS, quite quickly) translate to additional operating expenses 
(yes, there is also a cash aspect involved).
7. Extremely absurd clutter when looking at the data (even for 
[classes that have nothing to do with inheritance](#noinheritance): namely, 
that don't extend another entity/document, & are not extended) (which is actually also a big factor, once one takes a look at it): as can be seen 
[below](#list).
8. Issue 7 will most likely have a negative effect on developer & DB admin productivity: by inhibiting concentration on useful data due to presence of a lot of useless data.
9. Unnecessary tight-coupling of DB records to Java classes: a re-factoring of any @Document Java class to a different package (or changing the name of any Document 
class which already! has a customized! collection name) as of now would require running a query to update all relevant DB records (this is a major code smell & 
reveals that now there is a conflict (& bizarre duplication) between the inheritance-support implementation focusing on non-Documents & the semantics of @Document 
value attribute (the former prevents the latter from freely decoupling DB records from the name of Java class): the upstream project now forces updating all relevant 
DB records if the name of the class is changed).

### <a id="visuals"></a>Visual examples of optimized inefficiencies
#### <a id="single"></a>Single record

Absurd in upstream Spring Data ArangoDB:
![Alt text](docs/include/img/unreasonable.png?raw=true "Absurd")

Normal record provided with this implementation (the size is up to 3.69 times smaller (35/129 bytes)):
![Alt text](docs/include/img/reasonable.png?raw=true "Normal")

#### <a id="noinheritance"></a>A record for a class that DOESN'T extend another entity/document, & is not extended

Absurd in upstream Spring Data ArangoDB:
![Alt text](docs/include/img/aggregate_absurd.png?raw=true "Absurd")

Normal record provided with this implementation:
![Alt text](docs/include/img/aggregate.png?raw=true "Normal")

#### <a id="list"></a>A record for a class that has a property of type List with 2 entities/documents in it

Absurd in upstream Spring Data ArangoDB (with (automatic) join, in this case redundant data would be present in all [3 entities/documents](#multiples) that get retrieved):
![Alt text](docs/include/img/aggregate_with_collection_absurd.png?raw=true "Absurd")

Normal record provided with this implementation:
![Alt text](docs/include/img/aggregate_with_collection.png?raw=true "Normal")

### <a id="multiples"></a>Cumulative effect of optimizations (for JOINs, multiple records matching a query, etc.)
Taking the example of a [single record](#single) & estimating that the size of single record is 3.69 times smaller (35/129 bytes),
in each of the following also quite simple 2 examples (involving JOINS into 2 other COLLECTIONS) the effect would be cumulative 
(i.e., absolute size of data (stored, transferred, processed, etc.) would be multiplied by a factor of 3 (i.e., 1 + 1 + 1 or 1 + 2):

1. @Document
class A {
B b;
}
@Document
class B {
C c;
}
@Document
class C {
}

2. @Document
class D {
C c;
E e;
}
@Document
class C {
}
@Document
class E {
}

A.
If one adds to example 2. an eager retrieval of a simple List of instances of some class F of size 5, the cumulative effect would be
even more noticeable:

@Document
class D {
C c;
E e;
List&lt;F&gt; f;
}
@Document
class F {
}

So in this example, absolute size of data (stored, transferred, processed, etc.) would be multiplied by a factor of 8 
(i.e., 3 documents as in example 2. + 5 more for the list). Thus smaller size per record provides a cumulative effect for operations involving JOINs or multiple records matching a query, etc. (with propagating efficiencies & benefits in terms of memory, bandwidth, CPU, latency, operational expenses, productivity, 
as well as visual & perceptional aspects (simpler due to less clutter, less ambiguous), etc.).

### <a id="calc"></a>Cumulative efficiencies: simple sample calculations for various numbers of persisted entities
Assuming average record size difference to be as shown in example above for [single record](#single):
![Alt text](docs/include/img/efficiencies.png?raw=true "Efficiencies")

Conclusion: this implementation is significantly more efficient in terms of disk space, memory, bandwidth, & CPU usage, as well as in terms of latency, operational expenses, & productivity; & is better in terms of visual & perceptional aspects (simpler due to less clutter, less ambiguous), & in terms of 
DB records not being tightly-coupled with Java classes.

## <a name="testing"></a>Test report comparisons (showing that all upstream functionality is preserved, it is just optimized (not less, just better))
### Release 2.1.7 vs. 2.1.7.1-rational
[Modified (branch)](https://haqer1.github.io/arangodb-spring-data-rational/docs/branch/v2.1.7/auto-testing/modified/surefire-report.html)
[Upstream (original)](https://haqer1.github.io/arangodb-spring-data-rational/docs/branch/v2.1.7/auto-testing/original/surefire-report.html)
[Diff](https://haqer1.github.io/arangodb-spring-data-rational/docs/branch/v2.1.7/auto-testing/diff/)

### Optimization for edges and graph traversal branch
[Modified (branch)](https://haqer1.github.io/arangodb-spring-data-rational/docs/branch/optimization_for_edges_and_graph_traversal/auto-testing/modified/surefire-report.html)
[Upstream (original)](https://haqer1.github.io/arangodb-spring-data-rational/docs/branch/optimization_for_edges_and_graph_traversal/auto-testing/original/surefire-report.html)

### PR 41 vs. equivalent upstream 2.1.4-SNAPSHOT
[Modified (branch)](https://haqer1.github.io/arangodb-spring-data-rational/docs/branch/issue_40/auto-testing/modified/surefire-report.html)
[Upstream (original)](https://haqer1.github.io/arangodb-spring-data-rational/docs/branch/issue_40/auto-testing/original/surefire-report.html)
[Diff](https://haqer1.github.io/arangodb-spring-data-rational/docs/branch/issue_40/auto-testing/diff/)

## <a name="history"></a>Brief history
ArangoDB Spring Data had no support for inheritance in @Documents, so an [issue](https://github.com/arangodb/spring-data/issues/17#issue-304481714) was logged on 
March 13, 2018 focusing on support for a main-stream inheritance type: canonical COLLECTION-PER-CLASS (similar to TABLE-PER-CLASS in JPA). On March 24th, a pull request was provided for it. 
This pull request didn't receive the same quick treatment that others get. On April 5th, a strange 
[issue](https://github.com/arangodb/spring-data/issues/27#issue-311595550) was opened by 
another contributor to support inheritance in properties of interface type. That strange request was
followed by request to not merge the pull request for main-stream inheritance support of type COLLECTION-PER-CLASS. On April 12th, a pull request was submitted by 
that same contributor that focuses on
inheritance in non-@Documents by persisting the fully-qualified class name. On April 17th, despite it having been stated that for canonical COLLECTION-PER-CLASS type of inheritance
storing the fully-qualified class name is 100% unnecessary, that alternative PR got merged into upstream Spring Data ArangoDB. Despite the fact that the inefficiencies introduced by the 
alternative PR had been clearly shown, the maintainer of ArangoDB Spring Data refused to merge the original pull request (which had been updated to avoid persistence of the fully-qualified
class name for @Documents (because it's unnecessary & causes many issues & inefficiencies), leaving other cases as is (i.e., leaving them up to whatever ArangoDB Spring Data in general 
wants to do with them (such as based on the alternative PR))), & closed it on May 22nd. To make it clear, the developer of this fork never made a request to not merge the alternative PR, 
or to revert it: but the other developer requested the contributions here to not be merged, & that's how the PR got closed by the maintainer. Thus, to have rational
support for canonical COLLECTION-PER-CLASS type of inheritance, there is a need for a customized implementation.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.arangodb/arangodb-spring-data/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.arangodb/arangodb-spring-data)

- [Getting Started](docs/Drivers/SpringData/GettingStarted/README.md)
- [Reference](docs/Drivers/SpringData/Reference/README.md)

## Learn more
* [ArangoDB](https://www.arangodb.com/)
* [Demo](https://github.com/arangodb/spring-data-demo)
* [JavaDoc 1.0.0](http://arangodb.github.io/spring-data/javadoc-1_0/index.html)
* [JavaDoc 2.0.0](http://arangodb.github.io/spring-data/javadoc-2_0/index.html)
* [JavaDoc Java driver](http://arangodb.github.io/arangodb-java-driver/javadoc-4_3/index.html)
