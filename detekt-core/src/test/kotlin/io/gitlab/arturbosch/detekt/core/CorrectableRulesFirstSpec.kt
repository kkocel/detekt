package io.gitlab.arturbosch.detekt.core

import io.github.detekt.test.utils.compileForTest
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import io.gitlab.arturbosch.detekt.test.yamlConfig
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtClass
import org.junit.jupiter.api.Test

class CorrectableRulesFirstSpec {

    @Test
    fun `runs rule with id 'NonCorrectable' last`() {
        var actualLastRuleId = ""

        class First(config: Config) : Rule(config) {
            override val issue: Issue = Issue("NonCorrectable", "", Debt.FIVE_MINS)
            override fun visitClass(klass: KtClass) {
                actualLastRuleId = issue.id
            }
        }

        class Last(config: Config) : Rule(config) {
            override val issue: Issue = Issue("Correctable", "", Debt.FIVE_MINS)
            override fun visitClass(klass: KtClass) {
                actualLastRuleId = issue.id
            }
        }

        val testFile = path.resolve("Test.kt")
        val settings = createProcessingSettings(testFile, yamlConfig("configs/one-correctable-rule.yml"))
        val detector = Analyzer(
            settings,
            listOf(object : RuleSetProvider {
                override val ruleSetId: String = "Test"
                override fun instance(config: Config) = RuleSet(ruleSetId, listOf(Last(config), First(config)))
            }),
            emptyList()
        )

        settings.use { detector.run(listOf(compileForTest(testFile))) }

        assertThat(actualLastRuleId).isEqualTo("NonCorrectable")
    }
}
