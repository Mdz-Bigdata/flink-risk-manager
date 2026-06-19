package com.qinyadan.risk.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qinyadan.risk.web.entity.RiskResult;
import com.qinyadan.risk.web.mapper.RiskResultMapper;
import org.springframework.stereotype.Service;

@Service
public class RiskResultService extends ServiceImpl<RiskResultMapper, RiskResult> implements IService<RiskResult> {
}
