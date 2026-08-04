-- Full employee-to-cell mapping query.
-- This maps employees by employee_master.full_name and writes audit logs.
-- Run the whole script in PostgreSQL / SQuirreL.

BEGIN;

CREATE TEMP TABLE tmp_employee_cell_map_input (
    employee_name text NOT NULL,
    cell_id bigint NOT NULL
) ON COMMIT DROP;

INSERT INTO tmp_employee_cell_map_input (employee_name, cell_id)
VALUES
        ('Rohit Balasaheb Pansare', 27),
        ('Pradip Dattatraya Sangale', 27),
        ('Pravin Mane', 29),
        ('Sunil Tondlekar', 30),
        ('Sushil Khot', 30),
        ('Sandeep Banda Patil', 30),
        ('Suraj Bharucha', 32),
        ('Siddhesh Chavan', 33),
        ('Sanket Torne', 32),
        ('Sanjay Jadhav', 33),
        ('Rahul Devre', 30),
        ('Pralhad Tarakh', 30),
        ('Saurabh Jain Lunawat', 30),
        ('Milind Gondse', 30),
        ('Manoj Mali', 33),
        ('Prasanna Borkar', 30),
        ('Santosh Khairnar', 30),
        ('Yogesh Patil', 30),
        ('Atul Nehate', 30),
        ('Devendra Nilkute', 30),
        ('Mayur Ayare', 30),
        ('Sunil Sabale', 34),
        ('Ajay Anant Kulkarni', 35),
        ('Praksash Ganpatrao Bhukte', 35),
        ('Raghunandan Rao', 35),
        ('Mihir Rajendra Shah', 35),
        ('Prashant Dinkar Chavan', 35),
        ('Akshay Ramchandra Bharati', 35),
        ('Rohit Ramesh Mapankar', 35),
        ('Nikhil Chavan', 36),
        ('Ghanshyam Mohan Patel', 35),
        ('Girish Gosavi', 36),
        ('Vijay Tukaram Bhagate', 37),
        ('Nishant Ratnakar Sonawane', 37),
        ('Vishal Vishwajeet Mane', 32),
        ('Mansi Ashok Warang', 33),
        ('Sunita Sanjay Parde', 33),
        ('Roshan Namdev Gondhali', 33),
        ('Priyanka Anil Patil', 33),
        ('Sanjana Suresh Patil', 32),
        ('Manali Dilip Koyande', 32),
        ('Saanvi Sachin Mungekar', 32),
        ('Krishna Anant Moraye', 32),
        ('Vikas Namdeo Lalsare', 30),
        ('Firoj Sherkhan Pathan', 30),
        ('Rakesh Sureshrao Hiware', 30),
        ('Rahul Ashok Wagh', 30),
        ('Swapnil Nagraj Patil', 30),
        ('Sandip Pandurang Patil', 30),
        ('Manojkumar Rohidas Gund', 30),
        ('Deepak Ashok Patil', 30),
        ('Prathviraj Hanmantrao Biradar', 30),
        ('Rushikesh Adinath Dhole', 35),
        ('Mohammad Sohail Mohammad Ismail Ansari', 27),
        ('Satyajeet Sudhakar Mumbarkar', 38),
        ('Pratik Prakash Salvi', 39),
        ('Trupti Vasant Chavan', 35),
        ('KISHOR DATTARM NAGAP', 33),
        ('REENA PANDHARINATH RAWOOL', 33),
        ('AMIT BALKRISHNA SHINDE', 33),
        ('Yogesh Prakash shinde', 40),
        ('Sushil Gurunath Ayarkar', 40),
        ('Madhuri Pange', 41),
        ('Bhukya Naresh', 37),
        ('Kiran Madhukar Patil', 33),
        ('Harshala Suvarna Devde', 33),
        ('MAHESH SURESH KHANDEKAR', 37),
        ('Amol Ramchandra Kamble', 27),
        ('PRIYANKA PRABHAKAR JADHAV', 38),
        ('Kajal Yogiraj Bane', 38),
        ('Subhajit Chakraborty', 42),
        ('Shabbir Rahiman Shaikh', 33),
        ('Yogesh Bhadale', 27),
        ('ZAIDI ZARI HAIDER ZAIDI', 33),
        ('Arif Subhan Shaha', 35),
        ('Chaitali Patil', 43),
        ('Hariom Pandey', 33),
        ('Supriya Gujar', 42),
        ('MOHAMMED AVAISH QURESHI', 33),
        ('Rahul Narayan Nirmal', 33),
        ('Rahul Nathe', 34),
        ('RAJESH SUDHAKAR TANDEL', 37),
        ('SWATI RAHUL PATIL', 32),
        ('UDAY SAMBHAJI PATIL', 36),
        ('YOGESH HARIRAM RAHANGDALE', 33),
        ('Namdev Pawar', 32),
        ('Sachinkumar Ramrao Deore', 35),
        ('Ashish Ramashray Vishwakaram', 35),
        ('BHAGYASHREE YASHWANT RAORANE', 33),
        ('RANJEET PANDEY', 37),
        ('Asmita Pandurang Farakte', 33),
        ('Dipali Sonawane', 34),
        ('Karuna Wani', 43),
        ('Suman Rawat', 29),
        ('Pradeep Manchekar', 43),
        ('Anil Pardeshi', 33),
        ('Aakash Tiwari', 32),
        ('Ameya Sarvankar', 32),
        ('Rupesh Musale', 34),
        ('Suryakant Ubhe', 34),
        ('Paras Dhandhaliya', 34),
        ('Tushar Ughade', 33),
        ('Chetan Mundhe', 32),
        ('Minal Gharge', 34),
        ('Chittrasen Nishad', 44),
        ('Ashish Yadav', 34),
        ('Swapnil Jagtap', 32),
        ('Suraj Joshi', 33),
        ('Rahul Patil', 33),
        ('Jay Anil Jadhav', 38),
        ('Anuraag Singh', 45),
        ('Lochan Pagdhare', 32),
        ('Navnath Taware', 46),
        ('Tejas Date', 47),
        ('Danish Khan', 27),
        ('Sagar Nathe', 47),
        ('Sunita Khedkar', 33),
        ('Sonali Choughule', 33),
        ('Harshwardhan Bagde', 33),
        ('Rupali Kale', 33),
        ('Ganesh Lokhande', 44),
        ('Uttam Shirole', 42),
        ('Anil Ashokrao Tupkar', 32),
        ('Vishal Khaire', 32),
        ('MUTHUKODI KODI SHEKAR NADAR', 32),
        ('Nitin Kannaujiya', 29),
        ('Sameer Rane', 42),
        ('Mahesh Patil', 33),
        ('Haresh Rathod', 32),
        ('Rahul Surve', 48),
        ('Tejas Munj', 33),
        ('Md Jawed Alam', 37),
        ('Abhijit Deole', 32),
        ('Nikhil Narendra Patil', 33),
        ('Nikita Palve', 33),
        ('Sreejith Nair', 33),
        ('Neha Shah', 33),
        ('Durgesh Tupkar', 32),
        ('Bharat Kadam', 37),
        ('Vijeta Sunil Meshram', 33),
        ('Priyanka Jangam', 33),
        ('JIVAN CHANDU PURI', 45),
        ('Pravin Gurav', 37),
        ('Nandkishore Bhatkar', 32),
        ('Gaurav Amrutkar', 44),
        ('Kiran J. Pawar', 33),
        ('Pradeep Kailas Bhaskal', 32),
        ('CHETAN RAGHUNATH HADPE', 49),
        ('Dnyaneshwar Rathod', 42),
        ('Swapnil K. Gedam', 42),
        ('Mallikarjun Kopuri', 33),
        ('Vikas Kundalik Patil', 32),
        ('Santosh A. Satpute', 32),
        ('Pooja Misale', 33),
        ('Soniya Gaikwad', 47),
        ('ANKITA ROHIT KUMAR SINGH', 42),
        ('Shital Vinod Mohite', 33),
        ('Anirudha Vijay Rawat', 33),
        ('Barun Kamal Sharma', 32),
        ('Kiran Jadhav', 33),
        ('Vishal Sonalkar', 33),
        ('PRAVEEN RICHHARIYA', 42),
        ('SAGAR DNYANESHWAR MHASKAR', 33),
        ('SWATI GOPALRAO TANWAIS', 32),
        ('Mohini Maruti Jadhav', 27),
        ('Rohit Lokhande', 37),
        ('Prajakta Kothule', 33),
        ('VIKAS GOPINATH PANDEY', 33),
        ('Prabhudas Bhaskarao Bitra', 35),
        ('Parameshwar Attmaram Pawar', 35),
        ('Akshay Gajanan Janunkar', 35),
        ('SRISHTI MAHESHWARI', 33),
        ('PRASAD SUBHASH KULKARNI', 37),
        ('LINA MAHENDRA AHIRRAO', 33),
        ('Badiuz Zaman Ahmed', 37),
        ('Hruta Nileshbhai Desai', 32),
        ('SAURABH RAVINDRA YATAM', 32),
        ('Aniket Suhas Katare', 33),
        ('Roshani Ashok Dungu', 50),
        ('Prashant Prakash More', 33),
        ('Kaustubh Dattaram Bhosle', 43),
        ('Dinesh Jagannath Somwanshi', 29),
        ('Karamjit Singh Banwait', 33),
        ('Hozefa Mohammed Haveliwala', 37),
        ('Punam Nilesh Mahajan', 34),
        ('Parag Raju Adhave', 35),
        ('SHRUTI SHRIKANT NAMAYE', 32),
        ('Ritu Mahadev Tambe', 51),
        ('MANDAR VISHWANATH RANE', 33),
        ('Sarwesh Upadhyay', 42),
        ('Komal Mayekar', 34),
        ('PRAMOD HARIBHAU PATIL', 42),
        ('Rashi Trivedi', 42),
        ('Amol Rajendra Dandagavhal', 30),
        ('Rounak Patil', 33),
        ('Jitendra Mahesh Ghasle', 29),
        ('MAYURI VIKRAM PATIL', 33),
        ('Swapnil Kumar Rai', 42),
        ('Suresh Kisan Lahane', 42),
        ('KHUSALI OBHALIA', 33),
        ('Santosh Pal', 34),
        ('SAYALI GITARAM ABHANG', 33),
        ('GAJANAN SURESH THAKARE', 33),
        ('HARSHAL SUBHASH GAWAD', 48),
        ('Pratik Patil', 34),
        ('Swapnali Salvi', 33),
        ('PRATHMESH SHASHIKANT BAPARDEKAR', 50),
        ('NEHA SAMIR DURGE', 34),
        ('Hardik Joshi', 34),
        ('Prajakta Prabhakar Gawade', 35),
        ('Nilesh Pawar', 39),
        ('Priyanka Jijaba Sapkal', 50),
        ('Rekha Nitin Palve', 34),
        ('Mayur Devrao Sukale', 33),
        ('Shailesh Kumar Gupta', 42),
        ('Krishna Chaurasiya', 47),
        ('Akshay More', 34),
        ('Kaveri Kishor Khedekar', 50),
        ('Asmita Vitthal Yadav', 47),
        ('Vishal Ramdas Badhe', 32),
        ('RUTUJA DINKAR RANDHAWAN', 33),
        ('MAYURI NARESH TANDEL', 50),
        ('Tejas Tulshidas Gaikwad', 38),
        ('Prashant Vasant Dingankar', 27),
        ('Saurabh Sakharam Dhamne', 38),
        ('Ninad Kishor Alhat', 27),
        ('Subhash Vishwanath Shelke', 41),
        ('Ankita Sanjayrao Jadhav', 33),
        ('Neha Rahul Waghmare', 37),
        ('Mahesh Rasal', 52),
        ('Rhytham Gaikwad', 39),
        ('Prathamesh Raut', 49),
        ('Mukesh Sharma', 33),
        ('Gangadhar Yadavrao Londhe', 30),
        ('Ravindra Narayan Pawar', 30),
        ('Rupesh Vilas Kunkawlekar', 30),
        ('Prasad Purushottam Satpute', 36),
        ('Akshay Balaji Khorgade', 36),
        ('Isha Girish Shirke', 51),
        ('Jadhav Sharad Khandu', 38),
        ('Nilesh Uttam Borade', 52),
        ('Hardik Shripad Gund', 53),
        ('Malhar Kiran Kshirsagar', 54),
        ('Dhirendra Pratap Singh', 41),
        ('Chanchal kalyan Majumdar', 30),
        ('Atul Baburao Potbhare', 30),
        ('Harshal Mahesh Mhatre', 30),
        ('Meghesh Satyanarayan Gatla', 30),
        ('Ashwini Atmaram More', 51),
        ('Gajanan Sainath Kale', 30),
        ('Yogesh Bhanudas Nikam', 30),
        ('Kondba Ramrao Chopde', 30),
        ('Earesseril Bijesh Babu', 37),
        ('Sameer Subhash Patil', 32),
        ('Abhishek Vilas Dalvi', 38),
        ('Gangadhar Shankar Chavan', 30),
        ('Sayali Laxman Sawal', 32),
        ('Sachin Gangadhar Dongre', 30),
        ('Manish Gurunath Sedamkar', 45),
        ('Jatin Sudhakar Pimpale', 37),
        ('Sagar Mallappa Kumbhar', 37),
        ('Aditya Kumar', 37),
        ('Ashish Kumar Biswal', 37),
        ('Hemant Vishwas Sonawane', 37),
        ('Sonali Ravindra Sonawane', 37),
        ('Umesh Chandrakant Maragaje', 37),
        ('Pratiksha Prakash Gavhane', 37),
        ('Abdulkadar Naik', 35),
        ('Mehtab Salahuddin Qureshi', 35),
        ('Tushar Prakash Ganer', 35),
        ('Kiran Macchindra Kengar', 35),
        ('Mayuri Vijay Patil', 55),
        ('Satish Rajaram Salunkhe', 37),
        ('Dipak Namdev Bhosale', 35),
        ('Swapna Gupta', 50),
        ('Mangesh Dhuri', 37),
        ('Snehal Anil Mulik', 30);

-- Review this result first. It should return zero rows before/after mapping.
WITH normalized_input AS (
    SELECT DISTINCT
        employee_name,
        cell_id,
        upper(trim(regexp_replace(replace(replace(employee_name, chr(160), ' '), chr(194), ''), '[[:space:]]+', ' ', 'g'))) AS employee_name_key
    FROM tmp_employee_cell_map_input
), employee_match_counts AS (
    SELECT
        i.employee_name,
        i.cell_id,
        count(e.employee_id) AS employee_match_count,
        string_agg(e.employee_id::text, ', ' ORDER BY e.employee_id) AS employee_ids
    FROM normalized_input i
    LEFT JOIN employee_master e
      ON upper(trim(regexp_replace(replace(replace(coalesce(e.full_name, ''), chr(160), ' '), chr(194), ''), '[[:space:]]+', ' ', 'g'))) = i.employee_name_key
    GROUP BY i.employee_name, i.cell_id
), cell_status AS (
    SELECT
        i.employee_name,
        i.cell_id,
        CASE
            WHEN c.cell_id IS NULL THEN 'CELL_NOT_FOUND'
            WHEN upper(coalesce(c.active_flag, 'N')) <> 'Y' THEN 'CELL_INACTIVE'
            WHEN w.wing_id IS NULL THEN 'WING_NOT_FOUND'
            WHEN upper(coalesce(w.active_flag, 'N')) <> 'Y' THEN 'WING_INACTIVE'
            ELSE 'OK'
        END AS cell_status
    FROM normalized_input i
    LEFT JOIN m_cell_master c ON c.cell_id = i.cell_id
    LEFT JOIN m_wing_master w ON w.wing_id = c.wing_id
)
SELECT
    m.employee_name,
    m.cell_id,
    CASE
        WHEN m.employee_match_count = 0 THEN 'EMPLOYEE_NOT_FOUND'
        WHEN m.employee_match_count > 1 THEN 'EMPLOYEE_NAME_DUPLICATE'
        WHEN c.cell_status <> 'OK' THEN c.cell_status
        ELSE 'OK'
    END AS issue,
    m.employee_ids
FROM employee_match_counts m
JOIN cell_status c ON c.employee_name = m.employee_name AND c.cell_id = m.cell_id
WHERE m.employee_match_count <> 1 OR c.cell_status <> 'OK'
ORDER BY issue, m.employee_name;

WITH normalized_input AS (
    SELECT DISTINCT
        employee_name,
        cell_id,
        upper(trim(regexp_replace(replace(replace(employee_name, chr(160), ' '), chr(194), ''), '[[:space:]]+', ' ', 'g'))) AS employee_name_key
    FROM tmp_employee_cell_map_input
), employee_match_counts AS (
    SELECT
        i.employee_name,
        i.cell_id,
        i.employee_name_key,
        count(e.employee_id) AS employee_match_count
    FROM normalized_input i
    LEFT JOIN employee_master e
      ON upper(trim(regexp_replace(replace(replace(coalesce(e.full_name, ''), chr(160), ' '), chr(194), ''), '[[:space:]]+', ' ', 'g'))) = i.employee_name_key
    GROUP BY i.employee_name, i.cell_id, i.employee_name_key
), valid_cell AS (
    SELECT c.cell_id
    FROM m_cell_master c
    JOIN m_wing_master w ON w.wing_id = c.wing_id
    WHERE upper(coalesce(c.active_flag, 'N')) = 'Y'
      AND upper(coalesce(w.active_flag, 'N')) = 'Y'
), matched AS (
    SELECT
        e.employee_id,
        m.employee_name,
        m.cell_id
    FROM employee_match_counts m
    JOIN employee_master e
      ON upper(trim(regexp_replace(replace(replace(coalesce(e.full_name, ''), chr(160), ' '), chr(194), ''), '[[:space:]]+', ' ', 'g'))) = m.employee_name_key
    JOIN valid_cell vc ON vc.cell_id = m.cell_id
    WHERE m.employee_match_count = 1
), previous_mapping AS (
    SELECT
        m.employee_id,
        m.cell_id AS new_cell_id,
        existing.cell_id AS previous_cell_id
    FROM matched m
    LEFT JOIN employee_cell_mapping existing ON existing.employee_id = m.employee_id
), upserted AS (
    INSERT INTO employee_cell_mapping (
        employee_id,
        cell_id,
        created_date_time,
        updated_date_time
    )
    SELECT
        employee_id,
        cell_id,
        current_timestamp,
        current_timestamp
    FROM matched
    ON CONFLICT (employee_id)
    DO UPDATE SET
        cell_id = EXCLUDED.cell_id,
        updated_date_time = current_timestamp
    WHERE employee_cell_mapping.cell_id IS DISTINCT FROM EXCLUDED.cell_id
    RETURNING employee_id, cell_id
), audit_insert AS (
    INSERT INTO employee_cell_mapping_audit_log (
        employee_id,
        actor_login_id,
        action_type,
        previous_cell_id,
        new_cell_id,
        summary,
        details,
        occurred_at
    )
    SELECT
        u.employee_id,
        'DIRECT_SQL',
        CASE WHEN p.previous_cell_id IS NULL THEN 'ASSIGNED' ELSE 'UPDATED' END,
        p.previous_cell_id,
        u.cell_id,
        CASE WHEN p.previous_cell_id IS NULL
            THEN 'Employee cell assigned'
            ELSE 'Employee cell mapping updated'
        END,
        CASE WHEN p.previous_cell_id IS NULL
            THEN 'Cell assigned through direct SQL'
            ELSE 'Cell changed through direct SQL'
        END,
        current_timestamp
    FROM upserted u
    JOIN previous_mapping p ON p.employee_id = u.employee_id
    RETURNING audit_id
)
SELECT
    (SELECT count(*) FROM tmp_employee_cell_map_input) AS input_rows,
    (SELECT count(*) FROM matched) AS matched_valid_rows,
    (SELECT count(*) FROM upserted) AS inserted_or_updated_rows,
    (SELECT count(*) FROM audit_insert) AS audit_log_rows;

COMMIT;
