package com.forjrking.eventbus.post

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.LEFT
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionStatement
import com.intellij.ui.awt.RelativePoint
import com.intellij.usageView.UsageInfo
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.util.concurrency.AppExecutorUtil
import com.forjrking.eventbus.*
import com.forjrking.eventbus.subscribe.isSubscribe
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement

class PostLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val uElement = element.toUElement() ?: return null
        if (element !is PsiExpressionStatement) {
            val uCallExpression = uElement.getCallExpression()
            if (uCallExpression != null && uCallExpression.isPost()) {
                val psiIdentifier = uCallExpression.methodIdentifier?.sourcePsi ?: return null
                return PostLineMarkerInfo(psiIdentifier)
            }
        }
        return null
    }
}

internal fun UsageInfo.isPost(): Boolean {
    val uElement = element.toUElement()
    if (uElement != null) {
        if (uElement is UCallExpression) {
            return uElement.isPost()
        } else {
            return uElement.getParentOfTypeCallExpression()?.isPost() == true
        }
    }
    return false
}

private fun UCallExpression.isPost(): Boolean {
    return receiverType?.canonicalText == "org.greenrobot.eventbus.EventBus"
            && (methodName == "post" || methodName == "postSticky")
            && valueArgumentCount == 1
}

val icon = IconLoader.getIcon("/icons/near_me.svg")

private class PostLineMarkerInfo(
    psiElement: PsiElement
) : LineMarkerInfo<PsiElement>(
    psiElement,
    psiElement.textRange,
    icon,
    null,
    { event, element ->
        ReadAction.nonBlocking {
            var usages = emptyList<UsageInfo2UsageAdapter>()
            val uElement = element.toUElement()?.getParentOfType<UCallExpression>()
            if (uElement != null) {
                val argument = uElement.valueArguments.firstOrNull()
                val resolve = (argument?.getExpressionType() as PsiClassType).resolve()
                if (resolve != null) {
                    val collection = search(resolve)
                    usages = collection
                        .filter(UsageInfo::isSubscribe)
                        .map(::UsageInfo2UsageAdapter)
                }
            }
            blog("PostLineMarker - ${usages.size} usages found")
            ApplicationManager.getApplication().invokeLater {
                if (usages.size == 1) {
                    usages.first().navigate(true)
                } else {
                    showPostUsages(usages, RelativePoint(event))
                }
            }
        }.inSmartMode(element.project).submit(AppExecutorUtil.getAppExecutorService())
    },
    LEFT
)
