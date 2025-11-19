-- Hibernate Envers audit tables for contact entity
-- Revision info table
CREATE TABLE rev_info (
    id BIGSERIAL PRIMARY KEY,
    timestamp timestamp NOT NULL DEFAULT current_timestamp,
    username VARCHAR(100)
);

-- Audit table for contact (suffix _audit per configuration)
CREATE TABLE contact_audit (
    contact_id INT NOT NULL,
    rev_id BIGINT NOT NULL REFERENCES rev_info(id),
    rev_type SMALLINT NOT NULL,
    title VARCHAR(12),
    last_name VARCHAR(35) NOT NULL,
    first_name VARCHAR(35) NOT NULL,
    middle_names VARCHAR(35),
    date_of_birth DATE,
    deceased_date DATE,
    staff_flag BOOLEAN,
    remitter_flag BOOLEAN,
    gender VARCHAR(12),
    domestic_status VARCHAR(12),
    language_code VARCHAR(12),
    interpreter_required BOOLEAN,
    created_by VARCHAR(100),
    created_time TIMESTAMP,
    updated_by VARCHAR(100),
    updated_time TIMESTAMP,
    PRIMARY KEY (contact_id, rev_id)
);

CREATE INDEX idx_contact_audit_rev_id ON contact_audit(rev_id);
CREATE INDEX idx_contact_audit_contact_id ON contact_audit(contact_id);

