package com.madrapps.eventbus_nav.action

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ex.MessagesEx
import com.intellij.psi.PsiMethod
import com.intellij.util.FileContentUtilCore
import com.madrapps.eventbus_nav.util.ConfigHelper
import com.madrapps.eventbus_nav.util.fullName


/**
 * # MarkPosterAction
 * Created by 11324.
 * Date: 2019/10/8
 */
class MarkPosterAction : BaseMarkPosterAction() {

    override fun onPerform(method: PsiMethod) {

        val fullName = method.fullName ?: ""
        ConfigHelper.init(method.project)

        val result = MessagesEx.showOkCancelDialog(
                "Mark $fullName as a Poster Function?",
                "Mark Poster Function", null
        )
        if (result != Messages.OK) {
            return
        }
        ConfigHelper.postMethodSet.also { list ->
            list.add(fullName)
            ConfigHelper.postMethodSet = list
        }
        FileContentUtilCore.reparseFiles(method.containingFile.virtualFile)
    }

    override fun shouldShow(method: PsiMethod): Boolean {
        return method.fullName !in ConfigHelper.postMethodSet
    }

}