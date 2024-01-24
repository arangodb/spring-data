package arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.arangodb.springframework..", importOptions = {ImportOption.DoNotIncludeTests.class})
public class InternalsTest {
    @ArchTest
    public static final ArchRule noInternalDependency = noClasses().that()
            .resideInAPackage("com.arangodb.springframework..")
            .should().dependOnClassesThat()
            .areAssignableTo(resideInAPackage("..internal.."));

}
