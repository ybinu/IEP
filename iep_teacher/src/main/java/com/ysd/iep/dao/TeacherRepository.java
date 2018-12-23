package com.ysd.iep.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import javax.transaction.Transactional;

import com.ysd.iep.entity.Teachers;
import com.ysd.iep.entity.dto.TeacherDTO;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TeacherRepository extends JpaRepository<Teachers, String> , JpaSpecificationExecutor<Teachers> {

	@Query(value = "INSERT INTO teachertb (tea_id)VALUES(?1)", nativeQuery = true)
	@Modifying
	@Transactional
	public Integer addTecherById(String teaid);
	/**
	 * 所有查询教师Id
	 * @return
	 */
	@Query(value = "SELECT tea_id FROM teachertb", nativeQuery = true)
	public List<String> queryTeacherId();
	
	/**
	 * 所有查询教师信息
	 * @return
	 */
	@Query(value = "SELECT * FROM teachertb WHERE tea_id=?1", nativeQuery = true)
	public List<Object[]> queryTeacher(String teaId);
//	@Query(value = "SELECT * FROM teachertb WHERE tea_id=?1", nativeQuery = true)
//	public TeacherDTO queryTeacher(String teaId);
	
}
