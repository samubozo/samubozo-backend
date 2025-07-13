-- 부서 더미 데이터
INSERT INTO samubozo.tbl_departments (department_id, name, department_color) VALUES (1, '경영지원', '#FFAB91');
INSERT INTO samubozo.tbl_departments (department_id, name, department_color) VALUES (2, '인사팀', '#B39DDB');
INSERT INTO samubozo.tbl_departments (department_id, name, department_color) VALUES (3, '회계팀', '#81D4FA');
INSERT INTO samubozo.tbl_departments (department_id, name, department_color) VALUES (4, '영업팀', '#A5D6A7');

-- 직책 더미 데이터
INSERT INTO samubozo.tbl_position (position_id, position_name, hr_role) VALUES (1, '사장', 'Y');
INSERT INTO samubozo.tbl_position (position_id, position_name, hr_role) VALUES (2, '부장', 'Y');
INSERT INTO samubozo.tbl_position (position_id, position_name, hr_role) VALUES (3, '과장', 'N');
INSERT INTO samubozo.tbl_position (position_id, position_name, hr_role) VALUES (4, '대리', 'N');
INSERT INTO samubozo.tbl_position (position_id, position_name, hr_role) VALUES (5, '사원', 'N');