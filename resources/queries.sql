
{% sql/query upsert-subscription :1 %}

    insert into subscriptions ({% sql/cols fields %})
    values ({% sql/vals fields %})
    on conflict (feed_id, user_id)
    do update set {% sql/excluded fields %}, updated_at = now()
    returning *

{% sql/endquery %}


{% sql/query upsert-user :1 %}

    insert into users (email)
    values ({% sql/? email %})
    on conflict (email)
    do update set updated_at = now()
    returning *

{% sql/endquery %}


{% sql/query get-user-by-id :1 %}

    select * from users
    where id = {% sql/? id %}

{% sql/endquery %}


{% sql/query get-user-by-email :1 %}

    select * from users
    where email = {% sql/? email %}

{% sql/endquery %}


{% sql/query update-feed :1 %}

    update feed
    set {% sql/cols fields %}, updated_at = now()
    where id = {% sql/? id %}
    returning *

{% sql/endquery %}


{% sql/query reset-subscriptions-sync :count %}

    update
        subscriptions
    set
        sync_date_next = now(),
        updated_at = now()

{% sql/endquery %}



{% sql/query reset-feeds-sync :count %}

    update
        feeds
    set
        sync_date_next = now(),
        updated_at = now()

{% sql/endquery %}



{% sql/query sync-feed-ok :1 %}

    update feeds
    set
        {% sql/set fields %},
        updated_at = now(),
        err_attempts = 0,
        err_class = null,
        err_message = null,
        sync_count = sync_count + 1,
        sync_date_prev = now(),
        sync_date_next = now() + (interval '1 second' * sync_interval)

    where
        id = {% sql/? id %}

    returning *

{% sql/endquery %}


{% sql/query sync-feed-err :1 %}

    update feeds
    set
        updated_at = now(),
        err_attempts = err_attempts + 1,

        err_class = {% sql/? err-class %},
        err_message = {% sql/? err-message %},

        sync_count = sync_count + 1,
        sync_date_prev = now(),
        sync_date_next = now() + (interval '1 second' * sync_interval)

    where
        id = {% sql/? id %}

    returning *

{% sql/endquery %}



{% sql/query get-feed-by-id :1 %}

    select * from feeds
    where id = {% sql/? id %}

{% sql/endquery %}


{% sql/query upsert-entry :1 %}

    insert into entries (
        feed_id,
        guid,
        {% sql/cols fields %}
    )
    values (
        {% sql/? feed-id %},
        {% sql/? guid %},
        {% sql/vals fields %}
    )
    on conflict (feed_id, guid)
    do update set {% sql/excluded fields %}, updated_at = now()
    returning *

{% sql/endquery %}


{% sql/query upsert-feed :1 %}

    insert into feeds ({% sql/cols fields %})
    values ({% sql/vals fields %})
    on conflict (url_source)
    do update set {% sql/excluded fields %}, updated_at = now()
    returning *

{% sql/endquery %}


{% sql/query upsert-entries %}

    insert into entries ({% sql/cols* rows %})
    values {% sql/vals* rows %}
    on conflict (feed_id, guid)
    do update set {% sql/excluded* rows %}, updated_at = now()
    returning id, guid

{% sql/endquery %}



{% sql/query create-messages-for-subscription %}

    insert into messages(
        entry_id,
        subscription_id,
        date_published_at
    )
    select
        e.id,
        {% sql/? subscription-id %},
        e.date_published_at
    from
        entries e
    where
        e.feed_id = {% sql/? feed-id %}
    order by
        created_at desc
    limit
        {% sql/? limit %}
    on conflict (entry_id, subscription_id)
    do update set updated_at = now()
    returning *

{% sql/endquery %}


{% sql/query update-sync-next-for-subscription %}

    update
        subscriptions
    set
        sync_date_prev = now(),
        sync_date_next = now() + (interval '1 second' * sync_interval),
        sync_count = sync_count + 1,
        updated_at = now()
    where
        id = {% sql/? subscription-id %}
    returning
        *

{% sql/endquery %}


{% sql/query subscriptions-to-render %}

    select
        s.*,
        f.rss_title,
        f.url_source,
        f.url_website,
        f.rss_domain

    from
        subscriptions s,
        feeds f

    where
        s.user_id = {% sql/? user-id %}
        and s.feed_id = f.id

{% sql/endquery %}


{% sql/query messages-to-render %}

    select
        id,
        entry_id,
        is_read,
        is_marked,
        (extract(epoch from date_published_at)::text || '|' || id) as cursor

    from
        messages

    order by
        cursor {% if asc? %}asc{% else %}desc{% endif %}

    where
        not is_read
        and subscription_id = {% sql/? subscription-id %}
        {% if cursor %}
        and (extract(epoch from date_published_at)::text || '|' || id) {% if asc? %}>{% else %}<{% endif %} {% sql/? cursor %}
        {% endif %}

    limit
        {% sql/? limit %}

{% sql/endquery %}


{% sql/query get-entires-by-ids %}

    select
        id,
        title,
        link,
        teaser,
        date_published_at

    from
        entries

    where
        id in ({% sql/vals entry-ids %})

{% sql/endquery %}


{% sql/query feeds-to-update %}

    select
        id
    from
        feeds
    where
        sync_date_next is null
        or sync_date_next < now()
    order by
        sync_date_next asc nulls first
    limit
        {% sql/? limit %}

{% sql/endquery %}



{% sql/query subscriptions-to-update %}

    select
        id, feed_id
    from
        subscriptions
    where
        sync_date_next is null
        or sync_date_next < now()
    order by
        sync_date_next asc nulls first
    limit
        {% sql/? limit %}

{% sql/endquery %}


{% sql/query update-unread-for-subscription %}

    update
        subscriptions
    set
        unread_count = sub.unread
    from (
        select
            count(id) as unread
        from
            messages
        where
            subscription_id = {% sql/? subscription-id %}
            and not is_read
    ) as sub
    where
        id = {% sql/? subscription-id %}
    returning *


{% sql/endquery %}


{% sql/query expire-auth-codes %}

    delete from
        auth_codes
    where
        created_at < now() - interval '10 minutes'

{% sql/endquery %}



{% sql/query get-auth-code-by-id :1 %}

    select *
    from auth_codes
    where id = {% sql/? id %}

{% sql/endquery %}



{% sql/query upsert-categories %}

    insert into categories ({% sql/cols* rows %})
    values {% sql/vals* rows %}
    on conflict (parent_id, category)
    do update set updated_at = now()
    return *

{% sql/endquery %}
