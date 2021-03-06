package com.forjrking.eventbus.subscribe

import com.forjrking.eventbus.blog
import com.forjrking.eventbus.getParentOfTypeCallExpression
import com.forjrking.eventbus.post.isPost
import com.forjrking.eventbus.search
import com.forjrking.eventbus.showSubscribeUsages
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.LEFT
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.ui.awt.RelativePoint
import com.intellij.usageView.UsageInfo
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.uast.*
import org.jetbrains.uast.UastVisibility.PUBLIC

class SubscribeLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val uElement = element.toUElement() ?: return null
        val uMethod = uElement.getSubscribeMethod()
        if (uMethod != null) {
            val psiElement = uMethod.uastAnchor?.sourcePsi
            if (psiElement != null) {
                return SubscribeLineMarkerInfo(psiElement)
            }
        }
        return null
    }
}

internal fun UsageInfo.isSubscribe(): Boolean {
    val uElement = element.toUElement()
    if (uElement != null) {
        if (uElement.getParentOfType<UImportStatement>() == null) {
            val uMethod = uElement.getParentOfType<UMethod>()?.getSubscribeMethod()
            if (uMethod != null) {
                val qualifiedName =
                    uMethod.uastParameters.firstOrNull()?.type?.canonicalText ?: return false
                val qualifiedName1 =
                    uElement.getParentOfType<UTypeReferenceExpression>()?.getQualifiedName() ?: return false
                return qualifiedName == qualifiedName1
            }
        }
    }
    return false
}

private fun UElement.getSubscribeMethod(): UMethod? {
    if (this is UMethod) {
        annotations.find { it.qualifiedName == "org.greenrobot.eventbus.Subscribe" } ?: return null
        if (visibility == PUBLIC && uastParameters.size == 1) {
            return this
        }
    }
    return null
}

val icon = IconLoader.getIcon("/icons/bus.svg")

private class SubscribeLineMarkerInfo(
    psiElement: PsiElement
) : LineMarkerInfo<PsiElement>(
    psiElement,
    psiElement.textRange,
    icon,
    null,
    { event, element ->
        ReadAction.nonBlocking {
            var usages = emptyList<UsageInfo2UsageAdapter>()
            val uElement = element.toUElement()?.getParentOfType<UMethod>()
            if (uElement != null) {
                val elementToSearch =
                    (uElement.uastParameters[0].type as PsiClassReferenceType).reference.resolve()
                if (elementToSearch != null) {
                    val psiClassElement = elementToSearch.toUElement()
                    val collection = if ((psiClassElement as? PsiClass)?.isEnum == true) {
                        val elementsToSearch = psiClassElement.allFields.filterIsInstance<PsiEnumConstant>()
                        search(elementsToSearch)
                    } else {
                        search(elementToSearch)
                    }
                    val nonImports = collection.filter {
                        it.element.toUElement()?.getParentOfType<UImportStatement>() == null
                    }
                    val map = nonImports.flatMap {
                        val uuElement = it.element.toUElement()
                        if (uuElement != null) {
                            val parentOfTypeCallExpression = uuElement.getParentOfTypeCallExpression()
                            val some = if (parentOfTypeCallExpression != null) {
                                listOf(parentOfTypeCallExpression.sourcePsi)
                            } else {
                                val method = uuElement.uastParent?.uastParent as? UMethod
                                val sourcePsi = method?.sourcePsi
                                if (sourcePsi != null && method.returnTypeReference?.getQualifiedName() == (elementToSearch as? PsiClass)?.qualifiedName) {
                                    val list = search(sourcePsi).map { it.element }
                                    list
                                } else {
                                    listOf(uuElement.sourcePsi)
                                }
                            }
                            some
                        } else {
                            listOf(uuElement?.sourcePsi)
                        }
                    }.filterNotNull()
                        .distinct()
                        .map(::UsageInfo)
                    usages = map.filter(UsageInfo::isPost)
                        .map(::UsageInfo2UsageAdapter)
                }
            }
            blog("SubscribeLineMarker - ${usages.size} usages found")
            ApplicationManager.getApplication().invokeLater {
                if (usages.size == 1) {
                    usages.first().navigate(true)
                } else {
                    showSubscribeUsages(usages, RelativePoint(event))
                }
            }
        }.inSmartMode(element.project).submit(AppExecutorUtil.getAppExecutorService())
    },
    LEFT
)