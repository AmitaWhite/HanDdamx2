package com.white.handdam.global.security;

import com.white.handdam.member.entity.Role;

public record AuthMember(
        Long id, Role role) {

}
