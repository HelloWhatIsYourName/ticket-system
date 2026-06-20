UPDATE sys_menu
SET route_path = '/app/ai/chat',
    component = 'views/ai/chat',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'chat';

UPDATE sys_menu
SET route_path = '/app/tickets/my',
    component = 'views/tickets/my',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'tickets';

UPDATE sys_menu
SET route_path = '/app/tickets/assigned',
    component = 'views/tickets/assigned',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'agent-workbench';

UPDATE sys_menu
SET route_path = '/app/knowledge',
    component = 'views/knowledge',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'knowledge';

UPDATE sys_menu
SET route_path = '/app/system',
    component = 'views/system',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'users';

UPDATE sys_menu
SET route_path = '/app/admin/dashboard',
    component = 'views/admin/dashboard',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'dashboard';
