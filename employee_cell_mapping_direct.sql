-- Direct employee-to-cell mapping generated from pasted-text.txt.
-- Generated on 2026-08-04 23:12:46 +05:30.
-- Rows with blank cell names were skipped:
--   Sandeep Sunil Kshirsagar
--   Jay Sachin Uchagaonkar
--   Tushar Sanjay Gadekar
--   Rahul Suresh Gaikwad
--   Priyesh Pramod Chavan
--   Prathmesh Dilip Dubey
--   Valmiki Yadav
--   Apurva Shah
--   Ravi Ramdas Wanve
--   Shivam Dileep Bhosale
--   Amit Shyamsunder Dandawate
--   Ankit Ravindra Dashetwar
--   Kailas Nandaji Gite
--   Ajinkya Vijay Manapure
--   Laxmikant Balram Tripathi
--   Ashishkumar Umaprasad Pandey
--   Yogesh Dnyaneshwar Barvekar
--   Aniket Umakant Thakare
--   Vighnesh Yogesh Pathe
--   Ravi Harishchandra Maurya
--   Disha Ashok Khatu
--   Vaibhav Biradar
--   Vinayak Mukund Kawle
--   Vilas Hiraman Bharambe
--   Shefali Astopad Mandal
--   Bharti Sanjay Desai
--   Sunil Atmaram Dhaytonde
--   Santosh Appa Kalantre
--   Vinit Kirankumar Chauhan
--   Rohan Sitaram Ghule
--   Vaibhav Sopan Dokhale
--   Sanket Rajesh kadam
--   Ritesh Muralidhar Suryaji
--   Karan Devendra Adhau
--   Rushikesh Shankar Girhe
--   Mahavir Neminath Sawle
--   Nilesh Gajananrao Chavhan
--   Yogesh Arunrao Badge
--   Yogesh Dnyaneshwar Rohankar
--   Chandrashekhar Balkrishna Kulkarni
--   Dayandev Marotrav Shelke
--   Satish Tukaram Kharate
--   Satishkumar Gangadhar Ranvir
--   Suhas Kisanrao Deshmukh
--   Uttam Ganpati Shendge
--   Alankar Janardan Wadkar
--   Dhananjay Kishor Patil
--   Ruchir Ramachandra Thakur
--   Sharvari Hemant Deshpande
--   Amol Kisanji Ambalwar
--   Anilkumar Hemkishore Nagpure
--   Harshal Anilrao Hedau
--   Kiran Shriramji Bulkunde
--   Roshan Vidhyadhar Medpalliwar
--   Vipul Parashram Kothare
--   Namdev Nimba Patil
--   Nilesh Dattatray Karpe
--   Pareshkumar Diliprao Dhangar
--   Tushar Anil Puri
--   Vishal Bhatu Patil
--   Chetan Shashikanth Shirke
--   Santosh Dattatray Kumbhar
--   Shrikant Balkrishna Kulkarni
--   Shwetesh Sahebrao Moon
--   Pravin Suresh Lambe
--   Eknath Narayan Satpute
--   Vikram Balkrishna Patil
--   Shubham Narayan Ravan
--   Saurabh Gunderao Kulkarni
--   Kavita Mohan Ghare
--   Aditya Bhushan Shetkar
--   Vandan Mehta
--   Mahesh Prabhakar Anantul
--   Leena Karbhari Gagare
--   Saba Shaikh
--   Vijay Narayan DT
--   Akhilesh Kumar Shyam Dhar Vind
--   Sarojani Ajinkya Kele
--   Pratik Manojkumar Karathiya
--   Vishal Ambala
--   Nitesh Pal
--   Priyanka Vasantrao Bhagat
--   Ashish Mohanlal Gupta
--   Sayali Rajendra Patil
--   Mayur Sandip Kale
--   Sahil Santosh Kulkarni
--   Parimal Shyam Inamdar
--   Dhanashri Pavan Tibile
--   Ajit Shantaram Lad
--   Rushikesh Raghu Pol
--   Ashish Jaysingh Maurya
--   Juhi Shrimali
--   Sagar Subhash Kate
--   Rohit Rajendra Suryavanshi
--   Santanu Mondal
--   Sangram Pandurang Rajpure
--   Jagadish Madhukar Patil
--   Rajendra Kumar Nath
--   Sarmishtha Singh
--   Niranjan Mahesh Kabade
--   Raj Kumar
--   Mayur Ashok Gaikwad
--
-- IMPORTANT:
-- 1. Run this whole script in PostgreSQL.
-- 2. First result set shows validation issues. If there are issues, the DO block raises an exception and rolls back.
-- 3. Employee matching is done by normalized employee_master.full_name.

BEGIN;

CREATE TEMP TABLE tmp_employee_cell_direct_mapping (
    employee_name text NOT NULL,
    cell_id bigint NOT NULL
) ON COMMIT DROP;

INSERT INTO tmp_employee_cell_direct_mapping (employee_name, cell_id)
VALUES
        ('Rohit Balasaheb Pansare', 27), -- Network Infra Cell
        ('Pradip Dattatraya Sangale', 27), -- Network Infra Cell
        ('Pravin Mane', 29), -- State Election Commission Cell
        ('Sunil Tondlekar', 30), -- Field Operations Cell
        ('Sushil Khot', 30), -- Field Operations Cell
        ('Sandeep Banda Patil', 30), -- Field Operations Cell
        ('Suraj Bharucha', 32), -- MahaDBT Cell
        ('Siddhesh Chavan', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Sanket Torne', 32), -- MahaDBT Cell
        ('Sanjay Jadhav', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Rahul Devre', 30), -- Field Operations Cell
        ('Pralhad Tarakh', 30), -- Field Operations Cell
        ('Saurabh Jain Lunawat', 30), -- Field Operations Cell
        ('Milind Gondse', 30), -- Field Operations Cell
        ('Manoj Mali', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Prasanna Borkar', 30), -- Field Operations Cell
        ('Santosh Khairnar', 30), -- Field Operations Cell
        ('Yogesh Patil', 30), -- Field Operations Cell
        ('Atul Nehate', 30), -- Field Operations Cell
        ('Devendra Nilkute', 30), -- Field Operations Cell
        ('Mayur Ayare', 30), -- Field Operations Cell
        ('Sunil Sabale', 34), -- Digital Solutions, Development and O&M Cell
        ('Ajay Anant Kulkarni', 35), -- BPMS Cell
        ('Praksash Ganpatrao Bhukte', 35), -- BPMS Cell
        ('Raghunandan Rao', 35), -- BPMS Cell
        ('Mihir Rajendra Shah', 35), -- BPMS Cell
        ('Prashant Dinkar Chavan', 35), -- BPMS Cell
        ('Akshay Ramchandra Bharati', 35), -- BPMS Cell
        ('Rohit Ramesh Mapankar', 35), -- BPMS Cell
        ('Nikhil Chavan', 36), -- IWBP Cell
        ('Ghanshyam Mohan Patel', 35), -- BPMS Cell
        ('Girish Gosavi', 36), -- IWBP Cell
        ('Vijay Tukaram Bhagate', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Nishant Ratnakar Sonawane', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Vishal Vishwajeet Mane', 32), -- MahaDBT Cell
        ('Mansi Ashok Warang', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Sunita Sanjay Parde', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Roshan Namdev Gondhali', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Priyanka Anil Patil', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Sanjana Suresh Patil', 32), -- MahaDBT Cell
        ('Manali Dilip Koyande', 32), -- MahaDBT Cell
        ('Saanvi Sachin Mungekar', 32), -- MahaDBT Cell
        ('Krishna Anant Moraye', 32), -- MahaDBT Cell
        ('Vikas Namdeo Lalsare', 30), -- Field Operations Cell
        ('Firoj Sherkhan Pathan', 30), -- Field Operations Cell
        ('Rakesh Sureshrao Hiware', 30), -- Field Operations Cell
        ('Rahul Ashok Wagh', 30), -- Field Operations Cell
        ('Swapnil Nagraj Patil', 30), -- Field Operations Cell
        ('Sandip Pandurang Patil', 30), -- Field Operations Cell
        ('Manojkumar Rohidas Gund', 30), -- Field Operations Cell
        ('Deepak Ashok Patil', 30), -- Field Operations Cell
        ('Prathviraj Hanmantrao Biradar', 30), -- Field Operations Cell
        ('Rushikesh Adinath Dhole', 35), -- BPMS Cell
        ('Mohammad Sohail Mohammad Ismail Ansari', 27), -- Network Infra Cell
        ('Satyajeet Sudhakar Mumbarkar', 38), -- HR & Administration Cell
        ('Pratik Prakash Salvi', 39), -- RERA Cell
        ('Trupti Vasant Chavan', 35), -- BPMS Cell
        ('KISHOR DATTARM NAGAP', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('REENA PANDHARINATH RAWOOL', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('AMIT BALKRISHNA SHINDE', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Yogesh Prakash shinde', 40), -- OCC Support - Dedicated
        ('Sushil Gurunath Ayarkar', 40), -- OCC Support - Dedicated
        ('Madhuri Pange', 41), -- Digital Media, Outreach & Promotion Cell
        ('Bhukya Naresh', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Kiran Madhukar Patil', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Harshala Suvarna Devde', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('MAHESH SURESH KHANDEKAR', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Amol Ramchandra Kamble', 27), -- Network Infra Cell
        ('PRIYANKA PRABHAKAR JADHAV', 38), -- HR & Administration Cell
        ('Kajal Yogiraj Bane', 38), -- HR & Administration Cell
        ('Subhajit Chakraborty', 42), -- NonDBT & NonRTS Cell
        ('Shabbir Rahiman Shaikh', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Yogesh Bhadale', 27), -- Network Infra Cell
        ('ZAIDI ZARI HAIDER ZAIDI', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Arif Subhan Shaha', 35), -- BPMS Cell
        ('Chaitali Patil', 43), -- RERA Cell & RDD Cell
        ('Hariom Pandey', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Supriya Gujar', 42), -- NonDBT & NonRTS Cell
        ('MOHAMMED AVAISH QURESHI', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Rahul Narayan Nirmal', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Rahul Nathe', 34), -- Digital Solutions, Development and O&M Cell
        ('RAJESH SUDHAKAR TANDEL', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('SWATI RAHUL PATIL', 32), -- MahaDBT Cell
        ('UDAY SAMBHAJI PATIL', 36), -- IWBP Cell
        ('YOGESH HARIRAM RAHANGDALE', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Namdev Pawar', 32), -- MahaDBT Cell
        ('Sachinkumar Ramrao Deore', 35), -- BPMS Cell
        ('Ashish Ramashray Vishwakaram', 35), -- BPMS Cell
        ('BHAGYASHREE YASHWANT RAORANE', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('RANJEET PANDEY', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Asmita Pandurang Farakte', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Dipali Sonawane', 34), -- Digital Solutions, Development and O&M Cell
        ('Karuna Wani', 43), -- RERA Cell & RDD Cell
        ('Suman Rawat', 29), -- State Election Commission Cell
        ('Pradeep Manchekar', 43), -- RERA Cell & RDD Cell
        ('Anil Pardeshi', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Aakash Tiwari', 32), -- MahaDBT Cell
        ('Ameya Sarvankar', 32), -- MahaDBT Cell
        ('Rupesh Musale', 34), -- Digital Solutions, Development and O&M Cell
        ('Suryakant Ubhe', 34), -- Digital Solutions, Development and O&M Cell
        ('Paras Dhandhaliya', 34), -- Digital Solutions, Development and O&M Cell
        ('Tushar Ughade', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Chetan Mundhe', 32), -- MahaDBT Cell
        ('Minal Gharge', 34), -- Digital Solutions, Development and O&M Cell
        ('Chittrasen Nishad', 44), -- MahaAgritech Cell
        ('Ashish Yadav', 34), -- Digital Solutions, Development and O&M Cell
        ('Swapnil Jagtap', 32), -- MahaDBT Cell
        ('Suraj Joshi', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Rahul Patil', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Jay Anil Jadhav', 38), -- HR & Administration Cell
        ('Anuraag Singh', 45), -- MahaSamanvay (MHUCDH) Cell
        ('Lochan Pagdhare', 32), -- MahaDBT Cell
        ('Navnath Taware', 46), -- AgriDBT Cell
        ('Tejas Date', 47), -- Post & Pre Matric MahaDBT Cell
        ('Danish Khan', 27), -- Network Infra Cell
        ('Sagar Nathe', 47), -- Post & Pre Matric MahaDBT Cell
        ('Sunita Khedkar', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Sonali Choughule', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Harshwardhan Bagde', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Rupali Kale', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Ganesh Lokhande', 44), -- MahaAgritech Cell
        ('Uttam Shirole', 42), -- NonDBT & NonRTS Cell
        ('Anil Ashokrao Tupkar', 32), -- MahaDBT Cell
        ('Vishal Khaire', 32), -- MahaDBT Cell
        ('MUTHUKODI KODI SHEKAR NADAR', 32), -- MahaDBT Cell
        ('Nitin Kannaujiya', 29), -- State Election Commission Cell
        ('Sameer Rane', 42), -- NonDBT & NonRTS Cell
        ('Mahesh Patil', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Haresh Rathod', 32), -- MahaDBT Cell
        ('Rahul Surve', 48), -- Maitri Cell
        ('Tejas Munj', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Md Jawed Alam', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Abhijit Deole', 32), -- MahaDBT Cell
        ('Nikhil Narendra Patil', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Nikita Palve', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Sreejith Nair', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Neha Shah', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Durgesh Tupkar', 32), -- MahaDBT Cell
        ('Bharat Kadam', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Vijeta Sunil Meshram', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Priyanka Jangam', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('JIVAN CHANDU PURI', 45), -- MahaSamanvay (MHUCDH) Cell
        ('Pravin Gurav', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Nandkishore Bhatkar', 32), -- MahaDBT Cell
        ('Gaurav Amrutkar', 44), -- MahaAgritech Cell
        ('Kiran J. Pawar', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Pradeep Kailas Bhaskal', 32), -- MahaDBT Cell
        ('CHETAN RAGHUNATH HADPE', 49), -- CM Dashboard Cell
        ('Dnyaneshwar Rathod', 42), -- NonDBT & NonRTS Cell
        ('Swapnil K. Gedam', 42), -- NonDBT & NonRTS Cell
        ('Mallikarjun Kopuri', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Vikas Kundalik Patil', 32), -- MahaDBT Cell
        ('Santosh A. Satpute', 32), -- MahaDBT Cell
        ('Pooja Misale', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Soniya Gaikwad', 47), -- Post & Pre Matric MahaDBT Cell
        ('ANKITA ROHIT KUMAR SINGH', 42), -- NonDBT & NonRTS Cell
        ('Shital Vinod Mohite', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Anirudha Vijay Rawat', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Barun Kamal Sharma', 32), -- MahaDBT Cell
        ('Kiran Jadhav', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Vishal Sonalkar', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('PRAVEEN RICHHARIYA', 42), -- NonDBT & NonRTS Cell
        ('SAGAR DNYANESHWAR MHASKAR', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('SWATI GOPALRAO TANWAIS', 32), -- MahaDBT Cell
        ('Mohini Maruti Jadhav', 27), -- Network Infra Cell
        ('Rohit Lokhande', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Prajakta Kothule', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('VIKAS GOPINATH PANDEY', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Prabhudas Bhaskarao Bitra', 35), -- BPMS Cell
        ('Parameshwar Attmaram Pawar', 35), -- BPMS Cell
        ('Akshay Gajanan Janunkar', 35), -- BPMS Cell
        ('SRISHTI MAHESHWARI', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('PRASAD SUBHASH KULKARNI', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('LINA MAHENDRA AHIRRAO', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Badiuz Zaman Ahmed', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Hruta Nileshbhai Desai', 32), -- MahaDBT Cell
        ('SAURABH RAVINDRA YATAM', 32), -- MahaDBT Cell
        ('Aniket Suhas Katare', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Roshani Ashok Dungu', 50), -- Technical Manpower Supply Cell
        ('Prashant Prakash More', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Kaustubh Dattaram Bhosle', 43), -- RERA Cell & RDD Cell
        ('Dinesh Jagannath Somwanshi', 29), -- State Election Commission Cell
        ('Karamjit Singh Banwait', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Hozefa Mohammed Haveliwala', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Punam Nilesh Mahajan', 34), -- Digital Solutions, Development and O&M Cell
        ('Parag Raju Adhave', 35), -- BPMS Cell
        ('SHRUTI SHRIKANT NAMAYE', 32), -- MahaDBT Cell
        ('Ritu Mahadev Tambe', 51), -- Finance & Accounts Cell
        ('MANDAR VISHWANATH RANE', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Sarwesh Upadhyay', 42), -- NonDBT & NonRTS Cell
        ('Komal Mayekar', 34), -- Digital Solutions, Development and O&M Cell
        ('PRAMOD HARIBHAU PATIL', 42), -- NonDBT & NonRTS Cell
        ('Rashi Trivedi', 42), -- NonDBT & NonRTS Cell
        ('Amol Rajendra Dandagavhal', 30), -- Field Operations Cell
        ('Rounak Patil', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Jitendra Mahesh Ghasle', 29), -- State Election Commission Cell
        ('MAYURI VIKRAM PATIL', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Swapnil Kumar Rai', 42), -- NonDBT & NonRTS Cell
        ('Suresh Kisan Lahane', 42), -- NonDBT & NonRTS Cell
        ('KHUSALI OBHALIA', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Santosh Pal', 34), -- Digital Solutions, Development and O&M Cell
        ('SAYALI GITARAM ABHANG', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('GAJANAN SURESH THAKARE', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('HARSHAL SUBHASH GAWAD', 48), -- Maitri Cell
        ('Pratik Patil', 34), -- Digital Solutions, Development and O&M Cell
        ('Swapnali Salvi', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('PRATHMESH SHASHIKANT BAPARDEKAR', 50), -- Technical Manpower Supply Cell
        ('NEHA SAMIR DURGE', 34), -- Digital Solutions, Development and O&M Cell
        ('Hardik Joshi', 34), -- Digital Solutions, Development and O&M Cell
        ('Prajakta Prabhakar Gawade', 35), -- BPMS Cell
        ('Nilesh Pawar', 39), -- RERA Cell
        ('Priyanka Jijaba Sapkal', 50), -- Technical Manpower Supply Cell
        ('Rekha Nitin Palve', 34), -- Digital Solutions, Development and O&M Cell
        ('Mayur Devrao Sukale', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Shailesh Kumar Gupta', 42), -- NonDBT & NonRTS Cell
        ('Krishna Chaurasiya', 47), -- Post & Pre Matric MahaDBT Cell
        ('Akshay More', 34), -- Digital Solutions, Development and O&M Cell
        ('Kaveri Kishor Khedekar', 50), -- Technical Manpower Supply Cell
        ('Asmita Vitthal Yadav', 47), -- Post & Pre Matric MahaDBT Cell
        ('Vishal Ramdas Badhe', 32), -- MahaDBT Cell
        ('RUTUJA DINKAR RANDHAWAN', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('MAYURI NARESH TANDEL', 50), -- Technical Manpower Supply Cell
        ('Tejas Tulshidas Gaikwad', 38), -- HR & Administration Cell
        ('Prashant Vasant Dingankar', 27), -- Network Infra Cell
        ('Saurabh Sakharam Dhamne', 38), -- HR & Administration Cell
        ('Ninad Kishor Alhat', 27), -- Network Infra Cell
        ('Subhash Vishwanath Shelke', 41), -- Digital Media, Outreach & Promotion Cell
        ('Ankita Sanjayrao Jadhav', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Neha Rahul Waghmare', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Mahesh Rasal', 52), -- Capacity Building Cell
        ('Rhytham Gaikwad', 39), -- RERA Cell
        ('Prathamesh Raut', 49), -- CM Dashboard Cell
        ('Mukesh Sharma', 33), -- RTS for Aaple Sarkar & Website Developement Cell
        ('Gangadhar Yadavrao Londhe', 30), -- Field Operations Cell
        ('Ravindra Narayan Pawar', 30), -- Field Operations Cell
        ('Rupesh Vilas Kunkawlekar', 30), -- Field Operations Cell
        ('Prasad Purushottam Satpute', 36), -- IWBP Cell
        ('Akshay Balaji Khorgade', 36), -- IWBP Cell
        ('Isha Girish Shirke', 51), -- Finance & Accounts Cell
        ('Jadhav Sharad Khandu', 38), -- HR & Administration Cell
        ('Nilesh Uttam Borade', 52), -- Capacity Building Cell
        ('Hardik Shripad Gund', 53), -- State Recuitment Cell
        ('Malhar Kiran Kshirsagar', 54), -- RTS 2.0
        ('Dhirendra Pratap Singh', 41), -- Digital Media, Outreach & Promotion Cell
        ('Chanchal kalyan Majumdar', 30), -- Field Operations Cell
        ('Atul Baburao Potbhare', 30), -- Field Operations Cell
        ('Harshal Mahesh Mhatre', 30), -- Field Operations Cell
        ('Meghesh Satyanarayan Gatla', 30), -- Field Operations Cell
        ('Ashwini Atmaram More', 51), -- Finance & Accounts Cell
        ('Gajanan Sainath Kale', 30), -- Field Operations Cell
        ('Yogesh Bhanudas Nikam', 30), -- Field Operations Cell
        ('Kondba Ramrao Chopde', 30), -- Field Operations Cell
        ('Earesseril Bijesh Babu', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Sameer Subhash Patil', 32), -- MahaDBT Cell
        ('Abhishek Vilas Dalvi', 38), -- HR & Administration Cell
        ('Gangadhar Shankar Chavan', 30), -- Field Operations Cell
        ('Sayali Laxman Sawal', 32), -- MahaDBT Cell
        ('Sachin Gangadhar Dongre', 30), -- Field Operations Cell
        ('Manish Gurunath Sedamkar', 45), -- MahaSamanvay (MHUCDH) Cell
        ('Jatin Sudhakar Pimpale', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Sagar Mallappa Kumbhar', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Aditya Kumar', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Ashish Kumar Biswal', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Hemant Vishwas Sonawane', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Sonali Ravindra Sonawane', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Umesh Chandrakant Maragaje', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Pratiksha Prakash Gavhane', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Abdulkadar Naik', 35), -- BPMS Cell
        ('Mehtab Salahuddin Qureshi', 35), -- BPMS Cell
        ('Tushar Prakash Ganer', 35), -- BPMS Cell
        ('Kiran Macchindra Kengar', 35), -- BPMS Cell
        ('Mayuri Vijay Patil', 55), -- RDD Cell
        ('Satish Rajaram Salunkhe', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Dipak Namdev Bhosale', 35), -- BPMS Cell
        ('Swapna Gupta', 50), -- Technical Manpower Supply Cell
        ('Mangesh Dhuri', 37), -- Enterprise IT Infrastructure & Cloud Services Cell
        ('Snehal Anil Mulik', 30); -- Field Operations Cell

WITH normalized_mapping AS (
    SELECT
        employee_name,
        cell_id,
        upper(regexp_replace(replace(replace(employee_name, 'Â', ''), chr(160), ' '), '\s+', ' ', 'g')) AS employee_name_key
    FROM tmp_employee_cell_direct_mapping
), employee_match_counts AS (
    SELECT
        m.employee_name,
        m.cell_id,
        m.employee_name_key,
        count(e.employee_id) AS matched_employee_count
    FROM normalized_mapping m
    LEFT JOIN employee_master e
      ON upper(regexp_replace(replace(replace(coalesce(e.full_name, ''), 'Â', ''), chr(160), ' '), '\s+', ' ', 'g')) = m.employee_name_key
    GROUP BY m.employee_name, m.cell_id, m.employee_name_key
), input_conflicts AS (
    SELECT employee_name_key, string_agg(employee_name, ', ' ORDER BY employee_name) AS employee_name
    FROM normalized_mapping
    GROUP BY employee_name_key
    HAVING count(DISTINCT cell_id) > 1
), validation_errors AS (
    SELECT 'EMPLOYEE_NOT_FOUND' AS issue, employee_name, cell_id
    FROM employee_match_counts
    WHERE matched_employee_count = 0
    UNION ALL
    SELECT 'EMPLOYEE_NAME_NOT_UNIQUE' AS issue, employee_name, cell_id
    FROM employee_match_counts
    WHERE matched_employee_count > 1
    UNION ALL
    SELECT 'INPUT_NAME_MAPPED_TO_MULTIPLE_CELLS' AS issue, employee_name, NULL::bigint AS cell_id
    FROM input_conflicts
    UNION ALL
    SELECT 'CELL_NOT_FOUND_OR_INACTIVE' AS issue, m.employee_name, m.cell_id
    FROM normalized_mapping m
    LEFT JOIN m_cell_master c ON c.cell_id = m.cell_id
    LEFT JOIN m_wing_master w ON w.wing_id = c.wing_id
    WHERE c.cell_id IS NULL
       OR upper(coalesce(c.active_flag, 'N')) <> 'Y'
       OR w.wing_id IS NULL
       OR upper(coalesce(w.active_flag, 'N')) <> 'Y'
)
SELECT *
FROM validation_errors
ORDER BY issue, employee_name;

DO $validation$
DECLARE
    issue_count integer;
BEGIN
    WITH normalized_mapping AS (
        SELECT
            employee_name,
            cell_id,
            upper(regexp_replace(replace(replace(employee_name, 'Â', ''), chr(160), ' '), '\s+', ' ', 'g')) AS employee_name_key
        FROM tmp_employee_cell_direct_mapping
    ), employee_match_counts AS (
        SELECT
            m.employee_name,
            m.cell_id,
            m.employee_name_key,
            count(e.employee_id) AS matched_employee_count
        FROM normalized_mapping m
        LEFT JOIN employee_master e
          ON upper(regexp_replace(replace(replace(coalesce(e.full_name, ''), 'Â', ''), chr(160), ' '), '\s+', ' ', 'g')) = m.employee_name_key
        GROUP BY m.employee_name, m.cell_id, m.employee_name_key
    ), input_conflicts AS (
        SELECT employee_name_key
        FROM normalized_mapping
        GROUP BY employee_name_key
        HAVING count(DISTINCT cell_id) > 1
    ), validation_errors AS (
        SELECT 1
        FROM employee_match_counts
        WHERE matched_employee_count <> 1
        UNION ALL
        SELECT 1
        FROM input_conflicts
        UNION ALL
        SELECT 1
        FROM normalized_mapping m
        LEFT JOIN m_cell_master c ON c.cell_id = m.cell_id
        LEFT JOIN m_wing_master w ON w.wing_id = c.wing_id
        WHERE c.cell_id IS NULL
           OR upper(coalesce(c.active_flag, 'N')) <> 'Y'
           OR w.wing_id IS NULL
           OR upper(coalesce(w.active_flag, 'N')) <> 'Y'
    )
    SELECT count(*) INTO issue_count FROM validation_errors;

    IF issue_count > 0 THEN
        RAISE EXCEPTION 'Employee-cell mapping validation failed. Review the validation_errors result set above.';
    END IF;
END
$validation$;

WITH normalized_mapping AS (
    SELECT DISTINCT
        employee_name,
        cell_id,
        upper(regexp_replace(replace(replace(employee_name, 'Â', ''), chr(160), ' '), '\s+', ' ', 'g')) AS employee_name_key
    FROM tmp_employee_cell_direct_mapping
), matched AS (
    SELECT
        e.employee_id,
        m.employee_name,
        m.cell_id
    FROM normalized_mapping m
    JOIN employee_master e
      ON upper(regexp_replace(replace(replace(coalesce(e.full_name, ''), 'Â', ''), chr(160), ' '), '\s+', ' ', 'g')) = m.employee_name_key
), previous_mapping AS (
    SELECT matched.employee_id, mapping.cell_id AS previous_cell_id
    FROM matched
    LEFT JOIN employee_cell_mapping mapping ON mapping.employee_id = matched.employee_id
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
    (SELECT count(*) FROM tmp_employee_cell_direct_mapping) AS input_rows,
    (SELECT count(*) FROM matched) AS matched_rows,
    (SELECT count(*) FROM upserted) AS changed_rows,
    (SELECT count(*) FROM audit_insert) AS audit_rows;

COMMIT;
