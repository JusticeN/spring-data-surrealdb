package com.mss.surrealdbspringbootstarter.repository;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Registers a {@link SurrealRepositoryFactoryBean} bean definition for every
 * candidate interface extending {@link SurrealRepository} found in the
 * configured base packages.
 */
public class SurrealRepositoriesRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes attrs = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(EnableSurrealRepositories.class.getName()));

        Set<String> basePackages = new LinkedHashSet<>();
        if (attrs != null) {
            String[] declared = attrs.getStringArray("basePackages");
            for (String p : declared) {
                if (StringUtils.hasText(p)) {
                    basePackages.add(p);
                }
            }
        }
        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(metadata.getClassName()));
        }

        ClassPathScanningCandidateComponentProvider scanner = createScanner();

        for (String pkg : basePackages) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(pkg)) {
                String className = candidate.getBeanClassName();
                if (className == null) {
                    continue;
                }
                Class<?> repoInterface;
                try {
                    repoInterface = ClassUtils.forName(className, getClass().getClassLoader());
                } catch (ClassNotFoundException ex) {
                    throw new IllegalStateException("Cannot load repository interface " + className, ex);
                }
                if (!repoInterface.isInterface()) {
                    continue;
                }
                AbstractBeanDefinition def = BeanDefinitionBuilder
                        .genericBeanDefinition(SurrealRepositoryFactoryBean.class)
                        .addConstructorArgValue(repoInterface)
                        .addConstructorArgReference("surrealTemplate")
                        .getBeanDefinition();
                registry.registerBeanDefinition(buildBeanName(repoInterface), def);
            }
        }
    }

    private static ClassPathScanningCandidateComponentProvider createScanner() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(
                            org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition) {
                        return beanDefinition.getMetadata().isInterface()
                                && beanDefinition.getMetadata().isIndependent();
                    }
                };
        scanner.addIncludeFilter(new SurrealRepositoryTypeFilter());
        return scanner;
    }

    private static String buildBeanName(Class<?> repoInterface) {
        String simple = repoInterface.getSimpleName();
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }

    /**
     * Matches interfaces that (directly or transitively) extend
     * {@link SurrealRepository}.
     */
    private static class SurrealRepositoryTypeFilter implements TypeFilter {

        private static final String TARGET = SurrealRepository.class.getName();

        @Override
        public boolean match(MetadataReader reader, MetadataReaderFactory factory) throws IOException {
            if (!reader.getClassMetadata().isInterface()) {
                return false;
            }
            for (String iface : reader.getClassMetadata().getInterfaceNames()) {
                if (TARGET.equals(iface)) {
                    return true;
                }
                try {
                    MetadataReader parent = factory.getMetadataReader(iface);
                    if (match(parent, factory)) {
                        return true;
                    }
                } catch (IOException ignored) {
                    // unable to read parent metadata; skip
                }
            }
            return false;
        }
    }
}
