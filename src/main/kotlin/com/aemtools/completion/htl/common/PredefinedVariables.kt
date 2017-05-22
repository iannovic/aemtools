package com.aemtools.completion.htl.common

import com.aemtools.analysis.htl.callchain.typedescriptor.MergedTypeDescriptor
import com.aemtools.analysis.htl.callchain.typedescriptor.PredefinedTypeDescriptor
import com.aemtools.analysis.htl.callchain.typedescriptor.PropertiesTypeDescriptor
import com.aemtools.analysis.htl.callchain.typedescriptor.TypeDescriptor
import com.aemtools.analysis.htl.callchain.typedescriptor.TypeDescriptor.Companion.empty
import com.aemtools.analysis.htl.callchain.typedescriptor.java.JavaPsiClassTypeDescriptor
import com.aemtools.lang.htl.psi.mixin.VariableNameMixin
import com.aemtools.lang.htl.psi.util.elFields
import com.aemtools.lang.htl.psi.util.elMethods
import com.aemtools.lang.htl.psi.util.elName
import com.aemtools.lang.java.JavaSearch
import com.aemtools.service.ServiceFacade
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import java.util.*

/**
 * Provides completion on Htl context object (e.g. 'properties')
 * @author Dmytro_Troynikov
 */
object PredefinedVariables {

    private val repository = ServiceFacade.getHtlAttributesRepository()

    fun contextObjectsCompletion(): List<LookupElement> {
        return repository.getContextObjects().map {
            LookupElementBuilder.create(it.name)
                    .withTailText("(${it.className})", true)
                    .withTypeText("Context Object")
                    .withIcon(it.elementIcon)
        }
    }

    fun typeDescriptorByIdentifier(variableName: VariableNameMixin, project: Project): TypeDescriptor {
        val name = variableName.variableName()
        val classInfo = repository.findContextObject(name) ?: return TypeDescriptor.empty()
        val originalElement = variableName.originalElement
        val fullClassName = classInfo.className
        val psiClass = JavaSearch.findClass(fullClassName, project)
        val predefined = classInfo.predefined
        return when {
            name == "properties" && psiClass != null && predefined != null && predefined .isNotEmpty() -> {
                MergedTypeDescriptor(
                        PropertiesTypeDescriptor(originalElement),
                        PredefinedTypeDescriptor(predefined),
                        JavaPsiClassTypeDescriptor.create(psiClass)
                )
            }
            psiClass != null && predefined != null && predefined.isNotEmpty() -> {
                MergedTypeDescriptor(
                        PredefinedTypeDescriptor(predefined),
                        JavaPsiClassTypeDescriptor.create(psiClass)
                )
            }
            psiClass != null -> {
                JavaPsiClassTypeDescriptor.create(psiClass)
            }
            predefined != null && predefined.isNotEmpty() -> {
                PredefinedTypeDescriptor(predefined)
            }
            else -> empty()
        }
    }

    /**
     * Extract Htl applicable completion variants from class element.
     * @return list of LookupElements extracted from given class.
     */
    fun extractSuggestions(psiClass: PsiClass): List<LookupElement> {
        val methods = psiClass.elMethods()
        val fields = psiClass.elFields()

        val methodNames = ArrayList<String>()
        val result = ArrayList<LookupElement>()

        methods.forEach {
            var name = it.elName()
            if (methodNames.contains(name)) {
                name = it.name
            } else {
                methodNames.add(name)
            }
            var lookupElement = LookupElementBuilder.create(name)
                    .withIcon(it.getIcon(0))
                    .withTailText(" ${it.name}()", true)

            val returnType = it.returnType
            if (returnType != null) {
                lookupElement = lookupElement.withTypeText(returnType.presentableText, true)
            }

            result.add(lookupElement)
        }

        fields.forEach {
            val lookupElement = LookupElementBuilder.create(it.name.toString())
                    .withIcon(it.getIcon(0))
                    .withTypeText(it.type.presentableText, true)

            result.add(lookupElement)
        }

        return result
    }

}