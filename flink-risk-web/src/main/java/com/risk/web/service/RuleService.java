package com.risk.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.risk.web.entity.Rule;
import com.risk.web.mapper.RuleMapper;
import org.springframework.stereotype.Service;

@Service
public class RuleService extends ServiceImpl<RuleMapper, Rule> implements IService<Rule> {
}
