![ArangoDB-Logo](https://docs.arangodb.com/assets/arangodb_logo_2016_inverted.png)

# Spring Data ArangoDB Supporting COLLECTION-PER-CLASS Type of Inheritance Rationally 

One maintainer & 1 contributor in Spring Data ArangoDB project have refused to accept inheritance-related contributions implemented here. That decision has obviously
(& without doubt) been driven not by rational considerations about technology, but by something else. In the process of blocking the contributions implemented here 
Spring Data ArangoDB upstream project has become tainted by extremely severe inefficiencies & irrationality. The developer who has provided the inheritance-related
contributions implemented here, believes that what is now in the upstream is so irrational that it cannot be used as is, & therefore has to use a fork that 
provides rational & efficient implementation for main-stream persistence-related inheritance types like COLLECTION-PER-CLASS (TABLE-PER-CLASS in SQL jargon). 

* [Inefficiencies & other issues in Spring Data ArangoDB optimized by this implementation](#inefficiencies_optimized)
    * [Visual examples of optimized inefficiencies](#visuals)
       * [Single record](#single)
       * [A record for a class that doesn't extend another entity/document, & is not extended](#noinheritance)
       * [A record for a class that has a property of type List with 2 entities/documents in it](#list)
    * [Examples of optimized inefficiencies related to JOINS (with simple sample calculations)](#calc)
* [Brief history](#history)

## <a name="inefficiencies_optimized"></a>Inefficiencies & other issues in Spring Data ArangoDB OPTIMIZED/RESOLVED by this implementation
1. Data pollution & disk space waste: amount of data persisted/processed, etc. when using this implementation is between [3 and 26+ times smaller](#calc).
2. This data pollution & disk space waste in turn entail more memory utilization at run-time.
3. This also entails unnecessary band-width utilization.
4. All of the above also entail usage of more CPU cycles at run-time (considering storage of the unnecessary data, its retrieval, & processing).
5. Issues 1 -through- 4, (especially when using a Platform as a service) eventually (for a PaaS, quite quickly) translate to additional expenses (yes, there is also a cash aspect involved).
6. Extremely absurd clutter when looking at the data (even for [classes that have nothing to do with inheritance](#noinheritance): namely, that don't extend another entity/document, & are not extended) (which is actually also a big factor, once one takes a look at it): as can be seen [below](#list).
7. Unnecessary tight-coupling of DB records to Java classes: a re-factoring of any @Document Java class to a different package (or changing the name of any Document class which already! has a customized! collection name) as of now would require running a query to update all relevant DB records (this is a major code smell & reveals that now there is a conflict (& bizarre duplication) between the inheritance-support implementation focusing on non-Documents & the semantics of @Document value attribute (the former prevents the latter from freely decoupling DB records from the name of Java class): the upstream project now forces updating all relevant DB records if the name of the class is changed).

### <a id="visuals"></a>Visual examples of optimized inefficiencies
#### <a id="single"></a>Single record

Absurd in upstream Spring Data ArangoDB:
![Alt text](docs/img/unreasonable.png?raw=true "Absurd")

Normal record provided with this implementation (the size of is up to 3.69 times smaller (35/129 bytes)):
![Alt text](docs/img/reasonable.png?raw=true "Normal")

#### <a id="noinheritance"></a>A record for a class that doesn't extend another entity/document, & is not extended

Absurd in upstream Spring Data ArangoDB:
![Alt text](docs/img/aggregate_absurd.png?raw=true "Absurd")

Normal record provided with this implementation:
![Alt text](docs/img/aggregate.png?raw=true "Normal")

#### <a id="list"></a>A record for a class that has a property of type List with 2 entities/documents in it

Absurd in upstream Spring Data ArangoDB:
![Alt text](docs/img/aggregate_with_collection_absurd.png?raw=true "Absurd")

Normal record in provided with this implementation (with (automatic) join, the amount of data would be up to 11 times (3.69x3) smaller):
![Alt text](docs/img/aggregate_with_collection.png?raw=true "Normal")

### <a id="calc"></a>Examples of optimized inefficiencies related to JOINS (with simple sample calculations)
Taking the example of a [single record](#single) & estimating that the size of single record is 3.69 times smaller (35/129 bytes),
in each of the following also quite simple 2 examples (involving JOINS into 2 other COLLECTIONS) the amount of data returned (transferred, processed, etc.) could be estimated to be 3.69x3=11 times smaller:

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
If one adds to example 2. an eager retrieval of a simple List of instances of some class F of size 4, an additional estimated waste of space of 3.69*4=15 times is involved in upstream Spring Data ArangoDB:
@Document
class D {
C c;
E e;
List&lt;F&gt; f;
}
@Document
class F {
}

So in this example, the implementation provided here produces an amount of data that would be about 11+15=26 times smaller (with propagating efficiencies in terms of memory, bandwidth, CPU, operational expenses, visual benefits (simpler, less ambiguous), etc.)!

## <a name="history"></a>Brief history
ArangoDB Spring Data had no support for inheritance in @Documents, so an [issue](https://github.com/arangodb/spring-data/issues/17#issue-304481714) was logged on 
March 13, 2018 focusing on a main-stream inheritance support: COLLECTION-PER-CLASS. On March 24th, a pull request was provided for it. 
This pull request didn't receive the same quick treatment that others get. On April 5th, a strange 
[issue](https://github.com/arangodb/spring-data/issues/27#issue-311595550) was opened by 
another contributor to support inheritance in properties of interface type. That strange request was
followed by request to not merge the pull request for main-stream inheritance support of type COLLECTION-PER-CLASS. On April 12th, a pull request was submitted by 
that same contributor that focuses on
inheritance in non-@Documents by persisting the fully-qualified class name. On April 17th, despite it having been stated that for COLLECTION-PER-CLASS type of inheritance
storing the fully-qualified class name is 100% unnecessary, that alternative PR got merged into upstream Spring Data ArangoDB. Despite the fact that the inefficiencies introduced by the 
alternative PR have been clearly shown, the maintainer of ArangoDB Spring Data refused to merge the original pull request (which had been updated to avoid persistence of the fully-qualified
class name @Documents (because it's unnecessary & causes inefficiencies)), & closed it on May 22nd. Thus, to have rational
support for COLLECTION-PER-CLASS type of inheritance, there is a need for a customized implementation.


# Spring Data ArangoDB

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.arangodb/arangodb-spring-data/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.arangodb/arangodb-spring-data)

- [Getting Started](docs/Drivers/SpringData/GettingStarted/README.md)
- [Reference](docs/Drivers/SpringData/Reference/README.md)

## Learn more
* [ArangoDB](https://www.arangodb.com/)
* [Demo](https://github.com/arangodb/spring-data-demo)
* [JavaDoc 1.0.0](http://arangodb.github.io/spring-data/javadoc-1_0/index.html)
* [JavaDoc 2.0.0](http://arangodb.github.io/spring-data/javadoc-2_0/index.html)
* [JavaDoc Java driver](http://arangodb.github.io/arangodb-java-driver/javadoc-4_3/index.html)
