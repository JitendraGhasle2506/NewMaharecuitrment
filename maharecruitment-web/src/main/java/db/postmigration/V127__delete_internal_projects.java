package db.postmigration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V127__delete_internal_projects extends BaseJavaMigration {

    private static final String PROJECT_IDS = "3, 4, 5, 34, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 54";

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            clearEmployeeProjectMappings(statement);
            clearInternalVacancyReferences(statement);
            clearRecruitmentNotifications(statement);
            clearTeamAndPositionReferences(statement);
            deleteProjects(statement);
        }
    }

    private void clearEmployeeProjectMappings(Statement statement) throws Exception {
        statement.execute("""
                do $$
                begin
                    if to_regclass('employee_project_mapping') is not null then
                        delete from employee_project_mapping
                        where project_id in (
                            select project_id
                            from project_mst
                            where project_scope_type = 'INTERNAL'
                              and project_id in (%s)
                        );
                    end if;
                end $$;
                """.formatted(PROJECT_IDS));
    }

    private void clearInternalVacancyReferences(Statement statement) throws Exception {
        statement.execute("""
                do $$
                begin
                    if to_regclass('recruitment_notification') is not null
                       and exists (
                           select 1
                           from information_schema.columns
                           where table_schema = current_schema()
                             and table_name = 'recruitment_notification'
                             and column_name = 'internal_vacancy_opening_id'
                       ) then
                        update recruitment_notification
                        set internal_vacancy_opening_id = null
                        where internal_vacancy_opening_id in (
                            select internal_vacancy_opening_id
                            from internal_vacancy_opening
                            where project_id in (
                                select project_id
                                from project_mst
                                where project_scope_type = 'INTERNAL'
                                  and project_id in (%s)
                            )
                        );
                    end if;

                    if to_regclass('recruitment_assessment_feedback') is not null
                       and exists (
                           select 1
                           from information_schema.columns
                           where table_schema = current_schema()
                             and table_name = 'recruitment_assessment_feedback'
                             and column_name = 'internal_vacancy_opening_id'
                       ) then
                        update recruitment_assessment_feedback
                        set internal_vacancy_opening_id = null
                        where internal_vacancy_opening_id in (
                            select internal_vacancy_opening_id
                            from internal_vacancy_opening
                            where project_id in (
                                select project_id
                                from project_mst
                                where project_scope_type = 'INTERNAL'
                                  and project_id in (%s)
                            )
                        );
                    end if;
                end $$;
                """.formatted(PROJECT_IDS, PROJECT_IDS));

        deleteInternalVacancyChildRows(statement);

        statement.execute("""
                delete from internal_vacancy_opening
                where project_id in (
                    select project_id
                    from project_mst
                    where project_scope_type = 'INTERNAL'
                      and project_id in (%s)
                )
                """.formatted(PROJECT_IDS));
    }

    private void deleteInternalVacancyChildRows(Statement statement) throws Exception {
        statement.execute("""
                do $$
                begin
                    if to_regclass('internal_vacancy_interview_role') is not null then
                        delete from internal_vacancy_interview_role
                        where internal_vacancy_opening_id in (
                            select internal_vacancy_opening_id
                            from internal_vacancy_opening
                            where project_id in (
                                select project_id
                                from project_mst
                                where project_scope_type = 'INTERNAL'
                                  and project_id in (%s)
                            )
                        );
                    end if;

                    if to_regclass('internal_vacancy_interview_authority') is not null then
                        delete from internal_vacancy_interview_authority
                        where internal_vacancy_opening_id in (
                            select internal_vacancy_opening_id
                            from internal_vacancy_opening
                            where project_id in (
                                select project_id
                                from project_mst
                                where project_scope_type = 'INTERNAL'
                                  and project_id in (%s)
                            )
                        );
                    end if;

                    if to_regclass('internal_vacancy_interview_employee') is not null then
                        delete from internal_vacancy_interview_employee
                        where internal_vacancy_opening_id in (
                            select internal_vacancy_opening_id
                            from internal_vacancy_opening
                            where project_id in (
                                select project_id
                                from project_mst
                                where project_scope_type = 'INTERNAL'
                                  and project_id in (%s)
                            )
                        );
                    end if;

                    if to_regclass('internal_vacancy_opening_requirement') is not null then
                        delete from internal_vacancy_opening_requirement
                        where internal_vacancy_opening_id in (
                            select internal_vacancy_opening_id
                            from internal_vacancy_opening
                            where project_id in (
                                select project_id
                                from project_mst
                                where project_scope_type = 'INTERNAL'
                                  and project_id in (%s)
                            )
                        );
                    end if;

                    if to_regclass('internal_vacancy_replacement_employee') is not null then
                        delete from internal_vacancy_replacement_employee
                        where internal_vacancy_opening_id in (
                            select internal_vacancy_opening_id
                            from internal_vacancy_opening
                            where project_id in (
                                select project_id
                                from project_mst
                                where project_scope_type = 'INTERNAL'
                                  and project_id in (%s)
                            )
                        );
                    end if;
                end $$;
                """.formatted(PROJECT_IDS, PROJECT_IDS, PROJECT_IDS, PROJECT_IDS, PROJECT_IDS));
    }

    private void clearRecruitmentNotifications(Statement statement) throws Exception {
        statement.execute("""
                do $$
                begin
                    if to_regclass('recruitment_notification') is not null then
                        if to_regclass('agency_candidate_pre_onboarding') is not null then
                            update agency_candidate_pre_onboarding
                            set recruitment_interview_detail_id = null
                            where recruitment_interview_detail_id in (
                                select candidate.recruitment_interview_detail_id
                                from recruitment_interview_detail candidate
                                join recruitment_notification notification
                                  on notification.recruitment_notification_id = candidate.recruitment_notification_id
                                where notification.project_id in (
                                    select project_id
                                    from project_mst
                                    where project_scope_type = 'INTERNAL'
                                      and project_id in (%s)
                                )
                            );
                        end if;

                        if to_regclass('internal_vacancy_panel_assessment') is not null then
                            delete from internal_vacancy_panel_assessment
                            where recruitment_interview_detail_id in (
                                select candidate.recruitment_interview_detail_id
                                from recruitment_interview_detail candidate
                                join recruitment_notification notification
                                  on notification.recruitment_notification_id = candidate.recruitment_notification_id
                                where notification.project_id in (
                                    select project_id
                                    from project_mst
                                    where project_scope_type = 'INTERNAL'
                                      and project_id in (%s)
                                )
                            );
                        end if;

                        if to_regclass('recruitment_internal_level_two_feedback') is not null then
                            delete from recruitment_internal_level_two_feedback
                            where recruitment_internal_level_two_schedule_id in (
                                select schedule.recruitment_internal_level_two_schedule_id
                                from recruitment_internal_level_two_schedule schedule
                                join recruitment_interview_detail candidate
                                  on candidate.recruitment_interview_detail_id = schedule.recruitment_interview_detail_id
                                join recruitment_notification notification
                                  on notification.recruitment_notification_id = candidate.recruitment_notification_id
                                where notification.project_id in (
                                    select project_id
                                    from project_mst
                                    where project_scope_type = 'INTERNAL'
                                      and project_id in (%s)
                                )
                            );
                        end if;

                        if to_regclass('recruitment_internal_level_two_panel_member') is not null then
                            delete from recruitment_internal_level_two_panel_member
                            where recruitment_internal_level_two_schedule_id in (
                                select schedule.recruitment_internal_level_two_schedule_id
                                from recruitment_internal_level_two_schedule schedule
                                join recruitment_interview_detail candidate
                                  on candidate.recruitment_interview_detail_id = schedule.recruitment_interview_detail_id
                                join recruitment_notification notification
                                  on notification.recruitment_notification_id = candidate.recruitment_notification_id
                                where notification.project_id in (
                                    select project_id
                                    from project_mst
                                    where project_scope_type = 'INTERNAL'
                                      and project_id in (%s)
                                )
                            );
                        end if;

                        if to_regclass('recruitment_internal_level_two_schedule') is not null then
                            delete from recruitment_internal_level_two_schedule
                            where recruitment_interview_detail_id in (
                                select candidate.recruitment_interview_detail_id
                                from recruitment_interview_detail candidate
                                join recruitment_notification notification
                                  on notification.recruitment_notification_id = candidate.recruitment_notification_id
                                where notification.project_id in (
                                    select project_id
                                    from project_mst
                                    where project_scope_type = 'INTERNAL'
                                      and project_id in (%s)
                                )
                            );
                        end if;

                        if to_regclass('recruitment_external_interview_feedback') is not null then
                            delete from recruitment_external_interview_feedback
                            where recruitment_interview_detail_id in (
                                select candidate.recruitment_interview_detail_id
                                from recruitment_interview_detail candidate
                                join recruitment_notification notification
                                  on notification.recruitment_notification_id = candidate.recruitment_notification_id
                                where notification.project_id in (
                                    select project_id
                                    from project_mst
                                    where project_scope_type = 'INTERNAL'
                                      and project_id in (%s)
                                )
                            );
                        end if;

                        if to_regclass('recruitment_external_interview_panel_member') is not null then
                            delete from recruitment_external_interview_panel_member
                            where recruitment_interview_detail_id in (
                                select candidate.recruitment_interview_detail_id
                                from recruitment_interview_detail candidate
                                join recruitment_notification notification
                                  on notification.recruitment_notification_id = candidate.recruitment_notification_id
                                where notification.project_id in (
                                    select project_id
                                    from project_mst
                                    where project_scope_type = 'INTERNAL'
                                      and project_id in (%s)
                                )
                            );
                        end if;

                        if to_regclass('recruitment_assessment_feedback') is not null then
                            delete from recruitment_assessment_feedback
                            where recruitment_interview_detail_id in (
                                select candidate.recruitment_interview_detail_id
                                from recruitment_interview_detail candidate
                                join recruitment_notification notification
                                  on notification.recruitment_notification_id = candidate.recruitment_notification_id
                                where notification.project_id in (
                                    select project_id
                                    from project_mst
                                    where project_scope_type = 'INTERNAL'
                                      and project_id in (%s)
                                )
                            );
                        end if;

                        if to_regclass('recruitment_interview_detail') is not null then
                            delete from recruitment_interview_detail
                            where recruitment_notification_id in (
                                select recruitment_notification_id
                                from recruitment_notification
                                where project_id in (
                                    select project_id
                                    from project_mst
                                    where project_scope_type = 'INTERNAL'
                                      and project_id in (%s)
                                )
                            );
                        end if;

                        if to_regclass('agency_notification_tracking') is not null then
                            delete from agency_notification_tracking
                            where recruitment_notification_id in (
                                select recruitment_notification_id
                                from recruitment_notification
                                where project_id in (
                                    select project_id
                                    from project_mst
                                    where project_scope_type = 'INTERNAL'
                                      and project_id in (%s)
                                )
                            );
                        end if;

                        if to_regclass('recruitment_notification_agency_rank') is not null then
                            delete from recruitment_notification_agency_rank
                            where recruitment_notification_id in (
                                select recruitment_notification_id
                                from recruitment_notification
                                where project_id in (
                                    select project_id
                                    from project_mst
                                    where project_scope_type = 'INTERNAL'
                                      and project_id in (%s)
                                )
                            );
                        end if;

                        if to_regclass('recruitment_designation_vacancy') is not null then
                            delete from recruitment_designation_vacancy
                            where recruitment_notification_id in (
                                select recruitment_notification_id
                                from recruitment_notification
                                where project_id in (
                                    select project_id
                                    from project_mst
                                    where project_scope_type = 'INTERNAL'
                                      and project_id in (%s)
                                )
                            );
                        end if;

                        delete from recruitment_notification
                        where project_id in (
                            select project_id
                            from project_mst
                            where project_scope_type = 'INTERNAL'
                              and project_id in (%s)
                        );
                    end if;
                end $$;
                """.formatted(
                        PROJECT_IDS,
                        PROJECT_IDS,
                        PROJECT_IDS,
                        PROJECT_IDS,
                        PROJECT_IDS,
                        PROJECT_IDS,
                        PROJECT_IDS,
                        PROJECT_IDS,
                        PROJECT_IDS,
                        PROJECT_IDS,
                        PROJECT_IDS,
                        PROJECT_IDS,
                        PROJECT_IDS));
    }

    private void clearTeamAndPositionReferences(Statement statement) throws Exception {
        statement.execute("""
                do $$
                begin
                    if to_regclass('position_master') is not null then
                        update position_master
                        set project_id = null
                        where project_id in (
                            select project_id
                            from project_mst
                            where project_scope_type = 'INTERNAL'
                              and project_id in (%s)
                        );
                    end if;

                    if to_regclass('team_master') is not null then
                        update team_master
                        set project_id = null
                        where project_id in (
                            select project_id
                            from project_mst
                            where project_scope_type = 'INTERNAL'
                              and project_id in (%s)
                        );
                    end if;
                end $$;
                """.formatted(PROJECT_IDS, PROJECT_IDS));
    }

    private void deleteProjects(Statement statement) throws Exception {
        statement.execute("""
                delete from project_mst
                where project_scope_type = 'INTERNAL'
                  and project_id in (%s)
                """.formatted(PROJECT_IDS));
    }
}
