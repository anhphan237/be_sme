create table if not exists document_blocks (
    document_block_id varchar(36) primary key,
    company_id varchar(36) not null,
    document_id varchar(36) not null,
    parent_block_id varchar(36),
    block_type varchar(50) not null,
    props_json text,
    content_json text,
    order_key varchar(64) not null,
    status varchar(30) not null default 'ACTIVE',
    created_by varchar(36),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists ix_document_blocks_company_document
    on document_blocks(company_id, document_id);

create index if not exists ix_document_blocks_company_document_order
    on document_blocks(company_id, document_id, order_key);

create index if not exists ix_document_blocks_parent
    on document_blocks(company_id, parent_block_id);

alter table document_comments
    add column if not exists anchor_block_id varchar(36);

alter table document_comments
    add column if not exists anchor_start integer;

alter table document_comments
    add column if not exists anchor_end integer;

alter table document_comments
    add column if not exists anchor_text text;

create index if not exists ix_document_comments_anchor_block
    on document_comments(company_id, document_id, anchor_block_id);
