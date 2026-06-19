package com.qinyadan.risk.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 规则加载器
 * 从 YAML 配置文件加载规则并注册到规则引擎
 */
public class RuleLoader {
    private static final Logger logger = LoggerFactory.getLogger(RuleLoader.class);

    /**
     * 从 classpath YAML 文件加载规则
     */
    public static List<RuleConfig> loadFromYaml(String classpathResource) {
        List<RuleConfig> rules = new ArrayList<>();

        try (InputStream is = RuleLoader.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (is == null) {
                logger.warn("Rule config file not found: {}", classpathResource);
                return rules;
            }

            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(is);
            List<Map<String, Object>> ruleList = (List<Map<String, Object>>) root.get("rules");

            if (ruleList == null) {
                logger.warn("No rules defined in: {}", classpathResource);
                return rules;
            }

            for (Map<String, Object> item : ruleList) {
                RuleConfig config = parseRuleConfig(item);
                if (config != null) {
                    rules.add(config);
                }
            }

            logger.info("Loaded {} rules from {}", rules.size(), classpathResource);
        } catch (Exception e) {
            logger.error("Failed to load rules from: {}", classpathResource, e);
        }

        return rules;
    }

    /**
     * 解析单条规则配置
     */
    @SuppressWarnings("unchecked")
    private static RuleConfig parseRuleConfig(Map<String, Object> item) {
        try {
            String id = (String) item.get("id");
            String name = (String) item.get("name");
            String expression = (String) item.get("expression");
            String riskLevelStr = (String) item.get("risk_level");
            String action = (String) item.get("action");
            String description = (String) item.get("description");
            boolean enabled = item.get("enabled") == null || (boolean) item.get("enabled");
            int weight = item.get("weight") != null ? ((Number) item.get("weight")).intValue() : 1;

            if (id == null || expression == null) {
                logger.warn("Rule missing id or expression, skipping: {}", item);
                return null;
            }

            RiskLevel riskLevel = RiskLevel.valueOf(riskLevelStr);

            RuleConfig config = new RuleConfig(id, name, expression, riskLevel);
            config.setAction(action);
            config.setDescription(description);
            config.setEnabled(enabled);
            config.setWeight(weight);

            return config;
        } catch (Exception e) {
            logger.error("Failed to parse rule config: {}", item, e);
            return null;
        }
    }

    /**
     * 加载规则并注册到引擎
     */
    public static void loadAndRegister(AviatorRuleEngine engine, String classpathResource) {
        List<RuleConfig> rules = loadFromYaml(classpathResource);
        engine.registerRules(rules);
        logger.info("Registered {} rules into engine", engine.getRuleCount());
    }
}
