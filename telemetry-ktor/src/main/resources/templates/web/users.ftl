<#-- 用户管理模板 -->

<#assign title="CangJie 遥测管理后台 - 用户管理">
<#assign currentPage="users">
<#assign pageTitle="用户管理">

<#import "layout.ftl" as layout>

<@layout.page title=title currentPage=currentPage pageTitle=pageTitle additionalStyles='
<style>
    .user-card {
        border-radius: 1rem;
        border: none;
        box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
        margin-bottom: 1.5rem;
        transition: all 0.2s ease;
    }
    
    .user-card:hover {
        transform: translateY(-3px);
        box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15);
    }
    
    .user-card .card-header {
        background-color: transparent;
        border-bottom: 1px solid rgba(0, 0, 0, 0.05);
        padding: 1.25rem 1.5rem;
        font-weight: 600;
        display: flex;
        align-items: center;
        justify-content: space-between;
    }
    
    .user-card .card-body {
        padding: 1.5rem;
    }
    
    .user-avatar {
        width: 48px;
        height: 48px;
        border-radius: 50%;
        background-color: var(--bs-primary);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 1.5rem;
        margin-right: 1rem;
    }
    
    .user-info {
        display: flex;
        align-items: center;
    }
    
    .user-actions {
        display: flex;
        gap: 0.5rem;
    }
    
    .user-role {
        padding: 0.25rem 0.75rem;
        border-radius: 1rem;
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
    }
    
    .role-admin {
        background-color: rgba(var(--bs-primary-rgb), 0.1);
        color: var(--bs-primary);
    }
    
    .role-user {
        background-color: rgba(var(--bs-success-rgb), 0.1);
        color: var(--bs-success);
    }
    
    .form-control, .form-select {
        border-radius: 0.5rem;
        padding: 0.6rem 1rem;
    }
    
    .form-control:focus, .form-select:focus {
        box-shadow: 0 0 0 0.25rem rgba(67, 97, 238, 0.25);
        border-color: #4361ee;
    }
    
    .pagination {
        margin-bottom: 0;
    }
    
    .pagination .page-item .page-link {
        border-radius: 0.5rem;
        margin: 0 0.2rem;
        border: none;
        color: var(--bs-body-color);
    }
    
    .pagination .page-item.active .page-link {
        background-color: var(--bs-primary);
        color: #fff;
    }
    
    .pagination .page-item .page-link:hover {
        background-color: rgba(var(--bs-primary-rgb), 0.1);
    }
    
    .pagination .page-item.disabled .page-link {
        color: var(--bs-secondary);
        background-color: transparent;
    }
    
    .alert {
        border-radius: 0.75rem;
        border: none;
    }
    
    .table {
        border-collapse: separate;
        border-spacing: 0;
    }
    
    .table th {
        font-weight: 600;
        border-bottom-width: 1px;
    }
    
    .table td {
        vertical-align: middle;
    }
    
    .table-hover tbody tr:hover {
        background-color: rgba(var(--bs-primary-rgb), 0.05);
    }
    
    .status-badge {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        display: inline-block;
        margin-right: 0.5rem;
    }
    
    .status-active {
        background-color: var(--bs-success);
    }
    
    .status-inactive {
        background-color: var(--bs-danger);
    }
    
    .user-details {
        font-size: 0.875rem;
        color: var(--bs-secondary);
    }
    
    .user-details span {
        margin-right: 1rem;
    }
    
    .user-details i {
        margin-right: 0.25rem;
    }
    
    .search-box {
        position: relative;
    }
    
    .search-box .form-control {
        padding-left: 2.5rem;
    }
    
    .search-box i {
        position: absolute;
        left: 1rem;
        top: 50%;
        transform: translateY(-50%);
        color: var(--bs-secondary);
    }
</style>'>

    <#if success??>
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <div class="d-flex">
                <div class="me-3">
                    <i class="fas fa-check-circle fa-2x"></i>
                </div>
                <div>
                    <h5 class="alert-heading">操作成功</h5>
                    <p class="mb-0">${success}</p>
                </div>
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </#if>
    
    <#if error??>
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <div class="d-flex">
                <div class="me-3">
                    <i class="fas fa-exclamation-circle fa-2x"></i>
                </div>
                <div>
                    <h5 class="alert-heading">操作失败</h5>
                    <p class="mb-0">${error}</p>
                </div>
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </#if>

    <div class="user-card">
        <div class="card-header">
            <div>
                <i class="fas fa-users me-2 text-primary"></i> 用户列表
            </div>
            <div>
                <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#createUserModal">
                    <i class="fas fa-plus me-2"></i>添加用户
                </button>
            </div>
        </div>
        <div class="card-body">
            <div class="row mb-4">
                <div class="col-md-6">
                    <div class="search-box">
                        <i class="fas fa-search"></i>
                        <input type="text" class="form-control" id="searchInput" placeholder="搜索用户...">
                    </div>
                </div>
                <div class="col-md-6 text-end">
                    <span class="text-muted">共 ${totalCount} 个用户</span>
                </div>
            </div>
            
            <div class="table-responsive">
                <table class="table table-hover" id="usersTable">
                    <thead>
                        <tr>
                            <th>用户名</th>
                            <th>显示名称</th>
                            <th>角色</th>
                            <th>电子邮件</th>
                            <th>状态</th>
                            <th>创建时间</th>
                            <th>最后登录</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <#if users?? && users?size gt 0>
                            <#list users as user>
                                <tr>
                                    <td>${user.username}</td>
                                    <td>${user.displayName}</td>
                                    <td>
                                        <span class="user-role ${(user.role == 'ADMIN')?then('role-admin', 'role-user')}">
                                            ${user.role}
                                        </span>
                                    </td>
                                    <td>${user.email!''}</td>
                                    <td>
                                        <#if user.enabled>
                                            <span class="status-badge status-active"></span>启用
                                        <#else>
                                            <span class="status-badge status-inactive"></span>禁用
                                        </#if>
                                    </td>
                                    <td>
                                        <#if user.createdAt??>
                                            ${(user.createdAt?number_to_datetime)?string("yyyy-MM-dd HH:mm:ss")}
                                        <#else>
                                            -
                                        </#if>
                                    </td>
                                    <td>
                                        <#if user.lastLoginAt??>
                                            ${(user.lastLoginAt?number_to_datetime)?string("yyyy-MM-dd HH:mm:ss")}
                                        <#else>
                                            从未登录
                                        </#if>
                                    </td>
                                    <td>
                                        <div class="btn-group">
                                            <button class="btn btn-sm btn-outline-primary edit-user-btn" 
                                                    data-username="${user.username}"
                                                    data-displayname="${user.displayName}"
                                                    data-role="${user.role}"
                                                    data-email="${user.email!''}"
                                                    data-enabled="${user.enabled?string('true', 'false')}">
                                                <i class="fas fa-edit"></i>
                                            </button>
                                            <#if user.username != 'admin'>
                                                <button class="btn btn-sm btn-outline-danger delete-user-btn" 
                                                        data-username="${user.username}"
                                                        data-displayname="${user.displayName}">
                                                    <i class="fas fa-trash"></i>
                                                </button>
                                            </#if>
                                        </div>
                                    </td>
                                </tr>
                            </#list>
                        <#else>
                            <tr>
                                <td colspan="8" class="text-center py-4">
                                    <div class="text-muted">
                                        <i class="fas fa-info-circle me-2"></i>暂无用户数据
                                    </div>
                                </td>
                            </tr>
                        </#if>
                    </tbody>
                </table>
            </div>
            
            <#if totalPages gt 1>
                <div class="d-flex justify-content-between align-items-center mt-4">
                    <div>
                        显示 ${((currentPage - 1) * pageSize) + 1} - ${(currentPage * pageSize > totalCount)?then(totalCount, currentPage * pageSize)} 条，共 ${totalCount} 条
                    </div>
                    <nav aria-label="Page navigation">
                        <ul class="pagination">
                            <li class="page-item ${(currentPage == 1)?then('disabled', '')}">
                                <a class="page-link" href="/web/users?page=${currentPage - 1}" aria-label="Previous">
                                    <span aria-hidden="true">&laquo;</span>
                                </a>
                            </li>
                            
                            <#list 1..totalPages as page>
                                <#if page == currentPage>
                                    <li class="page-item active">
                                        <span class="page-link">${page}</span>
                                    </li>
                                <#elseif (page >= currentPage - 2 && page <= currentPage + 2) || page == 1 || page == totalPages>
                                    <li class="page-item">
                                        <a class="page-link" href="/web/users?page=${page}">${page}</a>
                                    </li>
                                <#elseif (page == currentPage - 3) || (page == currentPage + 3)>
                                    <li class="page-item disabled">
                                        <span class="page-link">...</span>
                                    </li>
                                </#if>
                            </#list>
                            
                            <li class="page-item ${(currentPage == totalPages)?then('disabled', '')}">
                                <a class="page-link" href="/web/users?page=${currentPage + 1}" aria-label="Next">
                                    <span aria-hidden="true">&raquo;</span>
                                </a>
                            </li>
                        </ul>
                    </nav>
                </div>
            </#if>
        </div>
    </div>
    
    <!-- 创建用户模态框 -->
    <div class="modal fade" id="createUserModal" tabindex="-1" aria-labelledby="createUserModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="createUserModalLabel">创建新用户</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <form action="/web/users/create" method="post">
                    <div class="modal-body">
                        <div class="mb-3">
                            <label for="username" class="form-label">用户名</label>
                            <input type="text" class="form-control" id="username" name="username" required>
                            <div class="form-text">用户名将用于登录，创建后不可更改</div>
                        </div>
                        <div class="mb-3">
                            <label for="password" class="form-label">密码</label>
                            <input type="password" class="form-control" id="password" name="password" required>
                        </div>
                        <div class="mb-3">
                            <label for="displayName" class="form-label">显示名称</label>
                            <input type="text" class="form-control" id="displayName" name="displayName" required>
                        </div>
                        <div class="mb-3">
                            <label for="role" class="form-label">角色</label>
                            <select class="form-select" id="role" name="role">
                                <option value="ADMIN">管理员</option>
                                <option value="USER" selected>普通用户</option>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label for="email" class="form-label">电子邮件</label>
                            <input type="email" class="form-control" id="email" name="email">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                        <button type="submit" class="btn btn-primary">创建</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    
    <!-- 编辑用户模态框 -->
    <div class="modal fade" id="editUserModal" tabindex="-1" aria-labelledby="editUserModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="editUserModalLabel">编辑用户</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <form action="/web/users/update" method="post">
                    <div class="modal-body">
                        <input type="hidden" id="editUsername" name="username">
                        <div class="mb-3">
                            <label for="editDisplayName" class="form-label">显示名称</label>
                            <input type="text" class="form-control" id="editDisplayName" name="displayName" required>
                        </div>
                        <div class="mb-3">
                            <label for="editRole" class="form-label">角色</label>
                            <select class="form-select" id="editRole" name="role">
                                <option value="ADMIN">管理员</option>
                                <option value="USER">普通用户</option>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label for="editEmail" class="form-label">电子邮件</label>
                            <input type="email" class="form-control" id="editEmail" name="email">
                        </div>
                        <div class="mb-3">
                            <label for="editPassword" class="form-label">密码</label>
                            <input type="password" class="form-control" id="editPassword" name="password">
                            <div class="form-text">留空表示不修改密码</div>
                        </div>
                        <div class="form-check form-switch mb-3">
                            <input class="form-check-input" type="checkbox" id="editEnabled" name="enabled">
                            <label class="form-check-label" for="editEnabled">启用账户</label>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                        <button type="submit" class="btn btn-primary">保存</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    
    <!-- 删除用户确认模态框 -->
    <div class="modal fade" id="deleteUserModal" tabindex="-1" aria-labelledby="deleteUserModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="deleteUserModalLabel">确认删除用户</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <p>您确定要删除用户 <span id="deleteUserName" class="fw-bold"></span> 吗？此操作不可撤销。</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                    <form action="/web/users/delete" method="post">
                        <input type="hidden" id="deleteUsername" name="username">
                        <button type="submit" class="btn btn-danger">删除</button>
                    </form>
                </div>
            </div>
        </div>
    </div>
    
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            // 搜索功能
            const searchInput = document.getElementById('searchInput');
            const usersTable = document.getElementById('usersTable');
            
            searchInput.addEventListener('keyup', function() {
                const searchValue = this.value.toLowerCase();
                const rows = usersTable.querySelectorAll('tbody tr');
                
                rows.forEach(row => {
                    const username = row.cells[0].textContent.toLowerCase();
                    const displayName = row.cells[1].textContent.toLowerCase();
                    const email = row.cells[3].textContent.toLowerCase();
                    
                    if (username.includes(searchValue) || displayName.includes(searchValue) || email.includes(searchValue)) {
                        row.style.display = '';
                    } else {
                        row.style.display = 'none';
                    }
                });
            });
            
            // 编辑用户
            const editUserButtons = document.querySelectorAll('.edit-user-btn');
            const editUserModal = new bootstrap.Modal(document.getElementById('editUserModal'));
            
            editUserButtons.forEach(button => {
                button.addEventListener('click', function() {
                    const username = this.getAttribute('data-username');
                    const displayName = this.getAttribute('data-displayname');
                    const role = this.getAttribute('data-role');
                    const email = this.getAttribute('data-email');
                    const enabled = this.getAttribute('data-enabled') === 'true';
                    
                    document.getElementById('editUsername').value = username;
                    document.getElementById('editDisplayName').value = displayName;
                    document.getElementById('editRole').value = role;
                    document.getElementById('editEmail').value = email;
                    document.getElementById('editPassword').value = '';
                    document.getElementById('editEnabled').checked = enabled;
                    
                    editUserModal.show();
                });
            });
            
            // 删除用户
            const deleteUserButtons = document.querySelectorAll('.delete-user-btn');
            const deleteUserModal = new bootstrap.Modal(document.getElementById('deleteUserModal'));
            
            deleteUserButtons.forEach(button => {
                button.addEventListener('click', function() {
                    const username = this.getAttribute('data-username');
                    const displayName = this.getAttribute('data-displayname');
                    
                    document.getElementById('deleteUsername').value = username;
                    document.getElementById('deleteUserName').textContent = displayName + ' (' + username + ')';
                    
                    deleteUserModal.show();
                });
            });
        });
    </script>
</@layout.page> 