package com.caiotcruz.mygamelist.infra.security;
@org.springframework.stereotype.Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.security.require-runtime-role",havingValue="true")
public class RuntimeRoleGuard implements org.springframework.boot.ApplicationRunner {
    private final org.springframework.jdbc.core.JdbcTemplate db;
    public RuntimeRoleGuard(org.springframework.jdbc.core.JdbcTemplate db){this.db=db;}
    public void run(org.springframework.boot.ApplicationArguments args){
        Boolean unsafe=db.queryForObject("SELECT rolsuper OR rolcreaterole OR rolcreatedb OR rolreplication OR rolbypassrls FROM pg_roles WHERE rolname=current_user",Boolean.class);
        Boolean owner=db.queryForObject("SELECT EXISTS(SELECT 1 FROM pg_database WHERE datname=current_database() AND datdba=(SELECT oid FROM pg_roles WHERE rolname=current_user)) OR EXISTS(SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relowner=(SELECT oid FROM pg_roles WHERE rolname=current_user))",Boolean.class);
        if(!Boolean.FALSE.equals(unsafe)||!Boolean.FALSE.equals(owner))throw new IllegalStateException("Hardened profile requires a non-owner, non-administrative database role");
    }
}
