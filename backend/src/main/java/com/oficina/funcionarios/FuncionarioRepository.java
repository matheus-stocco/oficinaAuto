package com.oficina.funcionarios;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    @Query("select f from Funcionario f where lower(f.login) = lower(:login)")
    Optional<Funcionario> buscarPorLogin(@Param("login") String login);

    /** Funcionários ativos com acesso, exceto o informado (regra do último acesso). */
    @Query("select count(f) from Funcionario f where f.ativo = true and f.login is not null and f.id <> :id")
    long contarOutrosComAcesso(@Param("id") Long id);

    long countByLoginIsNotNull();

    @Query("""
            select f from Funcionario f
            where lower(f.contato.nome) like lower(concat('%', :q, '%'))
               or f.contato.telefone like concat('%', :q, '%')
            """)
    Page<Funcionario> buscar(@Param("q") String q, Pageable pageable);
}
