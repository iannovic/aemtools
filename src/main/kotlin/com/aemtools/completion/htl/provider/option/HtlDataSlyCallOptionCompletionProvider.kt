package com.aemtools.completion.htl.provider.option

import com.aemtools.analysis.htl.callchain.typedescriptor.TemplateTypeDescriptor
import com.aemtools.completion.htl.inserthandler.HtlElAssignmentInsertHandler
import com.aemtools.completion.util.findChildrenByType
import com.aemtools.completion.util.findParentByType
import com.aemtools.lang.htl.psi.mixin.HtlElExpressionMixin
import com.aemtools.lang.htl.psi.mixin.PropertyAccessMixin
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.util.ProcessingContext

/**
 * @author Dmytro Troynikov
 */
object HtlDataSlyCallOptionCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
        if (result.isStopped) {
            return
        }

        val currentPosition = parameters.position
        val hel = currentPosition.findParentByType(HtlElExpressionMixin::class.java)
                ?: return

        val myPropertyChain = hel.findChildrenByType(PropertyAccessMixin::class.java)
                .firstOrNull() ?: return

        val accessChain = myPropertyChain.accessChain()
                ?: return
        val outputType = accessChain.callChainSegments
                .lastOrNull()?.outputType() as? TemplateTypeDescriptor
                ?: return
        val templateParameters = outputType.parameters()

        val presentOptions = hel.getOptions()
                .map { it.name() }
                .filterNot { it == "" }

        val variants = templateParameters
                .filterNot { presentOptions.contains(it) }
                .map {
                    LookupElementBuilder.create(it)
                            .withIcon(AllIcons.Nodes.Parameter)
                            .withTypeText("HTL Template Parameter")
                            .withInsertHandler(HtlElAssignmentInsertHandler())
                }

        result.addAllElements(variants)
        result.stopHere()
    }
}