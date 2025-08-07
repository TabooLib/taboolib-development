package org.tabooproject.development.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.*
import org.tabooproject.development.inlay.LangIndex
import org.tabooproject.development.isSendLangCall

/**
 * TabooLib 语言键缺失检查器
 * 
 * 检查代码中使用的语言键是否在语言文件中定义
 * 
 * @since 1.42
 */
class MissingLangKeyInspection : AbstractKotlinInspection() {

    override fun getDisplayName() = "语言键缺失检查"
    
    override fun getGroupDisplayName() = "TabooLib"
    
    override fun getShortName() = "MissingLangKey"
    
    override fun isEnabledByDefault() = true

    /**
     * 构建检查访问器
     */
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): KtVisitorVoid {
        return object : KtVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)
                
                // 检查是否是sendLang或asLangText调用
                if (!isSendLangCall(expression)) return
                
                // 获取第一个参数（语言键）
                val arguments = expression.valueArgumentList?.arguments
                if (arguments.isNullOrEmpty()) return
                
                val firstArg = arguments[0].getArgumentExpression()
                if (firstArg !is KtStringTemplateExpression) return
                
                // 提取语言键
                val langKey = extractStringLiteral(firstArg) ?: return
                
                // 检查语言键是否在语言文件中定义
                val project = expression.project
                val translation = LangIndex.getTranslation(project, langKey)
                
                if (translation == null) {
                    // 语言键不存在，报告问题
                    holder.registerProblem(
                        firstArg,
                        "语言键 '$langKey' 未在语言文件中定义",
                        ProblemHighlightType.WARNING,
                        AddLangKeyQuickFix(langKey)
                    )
                }
            }
        }
    }

    /**
     * 提取字符串字面量
     */
    private fun extractStringLiteral(stringTemplate: KtStringTemplateExpression): String? {
        val fullText = stringTemplate.text
        
        // 处理带引号的情况
        if ((fullText.startsWith("\"") && fullText.endsWith("\"")) || 
            (fullText.startsWith("'") && fullText.endsWith("'"))) {
            return fullText.substring(1, fullText.length - 1)
        }
        
        // 使用entries提取内容
        if (stringTemplate.entries.size == 1) {
            val entry = stringTemplate.entries[0]
            if (entry is KtLiteralStringTemplateEntry) {
                return entry.text
            }
        }
        
        return null
    }

    /**
     * 快速修复：在默认语言文件中添加缺失的语言键
     */
    class AddLangKeyQuickFix(private val langKey: String) : LocalQuickFix {
        
        override fun getName() = "添加语言键 '$langKey' 到默认语言文件"
        
        override fun getFamilyName() = "添加缺失的语言键"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            // 获取默认语言文件
            val defaultLangFile = LangIndex.getProjectDefaultLangFile(project)
            if (defaultLangFile == null) {
                return
            }
            
            ApplicationManager.getApplication().runWriteAction {
                // 读取当前文件内容
                val currentContent = String(defaultLangFile.contentsToByteArray(), Charsets.UTF_8)
                val contentBuilder = StringBuilder(currentContent)
                
                // 如果文件不以换行结束，添加换行
                if (!currentContent.endsWith("\n")) {
                    contentBuilder.append("\n")
                }
                
                // 添加新的语言键
                contentBuilder.append("\n# 自动添加的语言键\n")
                contentBuilder.append("${langKey}: \"${langKey}\"\n")
                
                // 写入文件
                defaultLangFile.setBinaryContent(contentBuilder.toString().toByteArray(Charsets.UTF_8))
            }
        }
    }
}