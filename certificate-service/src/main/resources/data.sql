-- 증명서 더미 데이터 10건 (certificate_id는 자동 증가)
INSERT INTO samubozo.tbl_certificates
(employee_no, type, status, purpose, request_date, approve_date) VALUES
                                                                     (1, 'EMPLOYMENT', 'REQUESTED', '은행제출용', '2025-07-10', NULL),
                                                                     (1, 'CAREER',    'APPROVED',  '이직용',   '2025-07-09', '2025-07-10'),
                                                                     (2, 'EMPLOYMENT', 'REJECTED', '비자발급', '2025-07-08', NULL),
                                                                     (2, 'CAREER',    'REQUESTED', '해외연수', '2025-07-07', NULL),
                                                                     (3, 'EMPLOYMENT', 'APPROVED', '기타',     '2025-07-06', '2025-07-07'),
                                                                     (3, 'CAREER',    'REQUESTED', '은행제출용', '2025-07-05', NULL),
                                                                     (4, 'EMPLOYMENT', 'APPROVED', '이직용',   '2025-07-04', '2025-07-05'),
                                                                     (5, 'CAREER',    'REJECTED',  '비자발급', '2025-07-03', NULL),
                                                                     (5, 'EMPLOYMENT', 'APPROVED', '해외연수', '2025-07-02', '2025-07-03'),
                                                                     (2, 'CAREER',    'REQUESTED', '기타',     '2025-07-01', NULL);