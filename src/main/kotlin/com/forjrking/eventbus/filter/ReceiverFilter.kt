package com.forjrking.eventbus.filter

import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMethod
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.forjrking.eventbus.util.firstParamTypeName
import com.forjrking.eventbus.util.toPsiMethod
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

//import org.jetbrains.kotlin.psi.KtNameReferenceExpression


class ReceiverFilter(private val paramType: String) : Filter {
    override fun shouldShow(usage: Usage): Boolean {
        val element = (usage as UsageInfo2UsageAdapter).element

        if (element is PsiJavaCodeReferenceElement) {
            val me = element.context?.context?.context ?: return false
            if (me is PsiMethod) {
                return me.firstParamTypeName() == paramType
            }
        }
        if (element is KtNameReferenceExpression) {
            val me = element.parent.parent.parent.parent.parent.parent
            if (me is KtNamedFunction) {
                return me.toPsiMethod()?.firstParamTypeName() == paramType
            }
        }
        return false
    }
}
