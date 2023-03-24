

{% sql/query upsert_user :1 %}

    insert into users ({% sql/cols fields %})
    values ({% sql/vals fields %})
    on conflict (email)
    do update set {% sql/excluded fields %}
    returning *

{% sql/endquery %}


{% sql/query get_user_by_id :1 %}

    select * from users
    where id = {% sql/? id %}

{% sql/endquery %}


{% sql/query update_feed :1 %}

    insert into feeds ({% sql/cols fields %})
    values ({% sql/vals fields %})
    on conflict (url_source)
    do update set {% sql/excluded fields %}
    returning *

{% sql/endquery %}


{% sql/query get_feed_by_id :1 %}

    select * from feeds
    where id = {% sql/? id %}

{% sql/endquery %}


{% sql/query upsert_entry :1 %}

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
    do update set {% sql/excluded fields %}
    returning *

{% sql/endquery %}


{% sql/query upsert_feed :1 %}

    insert into feeds (url_source, {% sql/cols fields %})
    values ({% sql/? url %}, {% sql/vals fields %})
    on conflict (url_source)
    do update set {% sql/excluded fields %}, updated_at = now()
    returning *

{% sql/endquery %}


{% sql/query upsert_entries %}

    insert into entries ({% sql/cols* rows %})
    values {% sql/vals* rows %}
    on conflict (feed_id, guid)
    do update set {% sql/excluded* rows %}
    returning id, guid

{% sql/endquery %}



{% sql/query create_messages_for_subscription %}

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
    do nothing
    returning *

{% sql/endquery %}


{% sql/query update_sync_next_for_subscription %}

    update
        subscriptions
    set
        sync_date_prev = now(),
        sync_date_next = now() + (interval '1 second' * sync_interval),
        sync_count = sync_count + 1
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


{% sql/query messages_to_render %}

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


{% sql/query get_entires_by_ids

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
