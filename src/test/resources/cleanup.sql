-- Clean up test data between tests
TRUNCATE TABLE delivery_method RESTART IDENTITY CASCADE;
TRUNCATE TABLE payment_method RESTART IDENTITY CASCADE;
