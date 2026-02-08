package com.blog.blog_backend.service;

import com.blog.blog_backend.model.dto.response.VisitorTrackResponse;
import com.blog.blog_backend.model.entity.Visitor;
import com.blog.blog_backend.repository.VisitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class VisitorService {

    @Autowired
    private VisitorRepository visitorRepository;
    
    public VisitorTrackResponse track(String clientId) {
        VisitorTrackResponse response = new VisitorTrackResponse();
        response.setSuccess(true);

        if ("unknown".equals(clientId)) {
            response.setMessage("IP를 확인할 수 없어 집계하지 않습니다.");
            return response;
        }

        LocalDate today = LocalDate.now();

        if (visitorRepository.existsByIpAddressAndVisitDate(clientId, today)) {
            response.setMessage("오늘 이미 방문했습니다.");
            return response;
        }

        Visitor visitor = new Visitor();
        visitor.setIpAddress(clientId);
        visitor.setVisitDate(today);
        visitor.setCreatedAt(LocalDateTime.now());
        visitorRepository.save(visitor);

        response.setMessage("방문자 수가 증가했습니다.");
        return response;
    }
}
