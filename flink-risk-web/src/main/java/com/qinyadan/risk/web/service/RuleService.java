package com.qinyadan.risk.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qinyadan.risk.web.entity.Rule;
import com.qinyadan.risk.web.mapper.RuleMapper;
import org.springframework.stereotype.Service;

@Service
public class RuleService extends ServiceImpl<RuleMapper, Rule> implements IService<Rule> {
}
