
delete from prisoner_contact where prisoner_contact_id in (
    select prisoner_contact_id
    from prisoner_contact pc
    where pc.contact_id in (30001, 30002, 30003, 30004, 30005, 30006)
);

delete from contact where contact_id in (30001, 30002, 30003, 30004, 30005, 30006);
