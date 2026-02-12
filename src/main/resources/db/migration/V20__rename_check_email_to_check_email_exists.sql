-- Rename permission code so operationType matches (check email exists)
UPDATE permissions
SET code = 'com.sme.identity.auth.checkEmailExists'
WHERE code = 'com.sme.identity.auth.checkEmail';
