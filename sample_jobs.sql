-- SQL Script to insert well-written sample jobs into the 'jobs' table
-- Use these to populate your application with realistic data

-- NOTE: Ensure you have users with IDs (e.g., user_id = 1) in your 'users' table before running this.
-- This script assumes your 'jobs' table structure includes the time-based fixes recently applied.

INSERT INTO jobs (title, company, location, description, category, salary_range, job_type, requirements, user_id, posted_date)
VALUES 
(
    'Senior Full Stack Developer', 
    'TechVision Solutions', 
    'Tunis, Tunisia', 
    'We are looking for an experienced Full Stack Developer to lead our core product team. You will be responsible for architecting scalable solutions using Spring Boot and React. The ideal candidate has a passion for clean code and mentoring junior developers.', 
    'Software Development', 
    '3500 - 5000 TND', 
    'Full-time', 
    '5+ years experience, Java proficiency, React expert, SQL mastery, Team leadership', 
    1, 
    NOW()
),
(
    'UI/UX Designer', 
    'Creative Pulse Agency', 
    'Sousse, Tunisia', 
    'Join our award-winning design team to create beautiful, user-centric mobile and web interfaces. You will work closely with product managers to translate complex requirements into intuitive designs. Proficiency in Figma and Adobe Creative Suite is a must.', 
    'Design', 
    '2000 - 3000 TND', 
    'Full-time', 
    'Portfolio of web/mobile work, Figma expertise, User research skills, Prototyping', 
    1, 
    DATE_SUB(NOW(), INTERVAL 2 HOUR)
),
(
    'Digital Marketing Specialist', 
    'GrowthHackers', 
    'Remote', 
    'Drive our online presence through strategic SEO, SEM, and social media campaigns. We need a data-driven individual who can analyze performance metrics and optimize conversion funnels. Experience with Google Analytics and Facebook Ads Manager is required.', 
    'Marketing', 
    '1500 - 2500 TND', 
    'Contract', 
    'SEO/SEM experience, Content strategy, Data analytics, Social media management', 
    1, 
    DATE_SUB(NOW(), INTERVAL 1 DAY)
),
(
    'Business Analyst Intern', 
    'Global Finance Corp', 
    'Tunis, Tunisia', 
    'Excellent opportunity for a student or recent graduate to gain hands-on experience in business process modeling and data analysis. You will assist our senior analysts in preparing executive reports and conducting market research.', 
    'Business', 
    '500 - 800 TND', 
    'Internship', 
    'Current student or graduate, Analytical mindset, Strong Excel skills, Good communication', 
    1, 
    DATE_SUB(NOW(), INTERVAL 4 HOUR)
),
(
    'Freelance Content Writer', 
    'EduStream Africa', 
    'Tunis (Remote Optional)', 
    'Looking for a versatile writer to produce high-quality educational blog posts and technical documentation. Must be able to simplify complex topics for a general audience while maintaining technical accuracy. English and French proficiency required.', 
    'Other', 
    '800 - 1200 TND', 
    'Freelance', 
    'Strong writing portfolio, Educational background preferred, SEO basics, Bilingual (EN/FR)', 
    1, 
    DATE_SUB(NOW(), INTERVAL 1 MONTH)
);
