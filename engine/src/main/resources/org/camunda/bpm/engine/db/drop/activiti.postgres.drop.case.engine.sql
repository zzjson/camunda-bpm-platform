drop index ACT_IDX_CASE_EXEC_BUSKEY;

drop index ACT_IDX_CASE_EXE_CASE_INST;
drop index ACT_IDX_CASE_EXE_PARENT;
drop index ACT_IDX_CASE_EXE_SUPER;
drop index ACT_IDX_CASE_EXE_CASE_DEF;
drop index ACT_IDX_VAR_CASE_EXE;
drop index ACT_IDX_VAR_CASE_INST_ID;
drop index ACT_IDX_TASK_CASE_EXEC;
drop index ACT_IDX_TASK_CASE_DEF_ID;

alter table ACT_RE_CASE_DEF
    drop CONSTRAINT ACT_UNIQ_CASE_DEF;

alter table ACT_RU_CASE_EXECUTION
    drop CONSTRAINT ACT_FK_CASE_EXE_CASE_INST;

alter table ACT_RU_CASE_EXECUTION
    drop CONSTRAINT ACT_FK_CASE_EXE_PARENT;

alter table ACT_RU_CASE_EXECUTION
    drop CONSTRAINT ACT_FK_CASE_EXE_SUPER;

alter table ACT_RU_CASE_EXECUTION
    drop CONSTRAINT ACT_FK_CASE_EXE_CASE_DEF;

alter table ACT_RU_VARIABLE
    drop CONSTRAINT ACT_FK_VAR_CASE_EXE;

alter table ACT_RU_VARIABLE
    drop CONSTRAINT ACT_FK_VAR_CASE_INST;

alter table ACT_RU_TASK
    drop CONSTRAINT ACT_FK_TASK_CASE_EXE;

alter table ACT_RU_TASK
    drop CONSTRAINT ACT_FK_TASK_CASE_DEF;

drop table ACT_RE_CASE_DEF;
drop table ACT_RU_CASE_EXECUTION;