/**
 * What the heeeeeeell!
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 * hello?
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
@Controller
@DefaultExceptionHandler
@RequestMapping("project/{userAlias}/{projectAlias}/blob")
public class BlobController extends AbstractRepositoryController {
    private final CodeHighLightService codeHighLightService;
    private final SettingService settingService;
    private final MarkdownService markdownService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobController.class);
    @Autowired
    public BlobController(
        BranchService branchService,
        RepositoryService repositoryService,
        UserProjectAccessLevelService userProjectAccessLevelService,
        CodeHighLightService codeHighLightService,
        RepositorySecurityService repositorySecurityService,
        SettingService settingService, MarkdownService markdownService
    ) {
        super(branchService, repositoryService, repositorySecurityService,  userProjectAccessLevelService);
        this.codeHighLightService = codeHighLightService;
        this.settingService = settingService;
        this.markdownService = markdownService;
    }
    @GetMapping
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ModelAndView read(
        @RequestParam(value = "file", required = false) String filePath,
        @RequestParam(value = "branch", required = false) String branch,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "plain", required = false ) String plain,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        if (Objects.isNull(filePath) || filePath.isEmpty())
            return RedirectUtil.redirect(String.format("/project/%s/%s?branch=%s",
                                                    alias.getValue(),
                                                    project.getAlias(),
                                                    branch
                                                ));
        BranchTuple baseBranch = getBranch(repository, project, branch);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit for branch %s", branch)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        @Nullable CommitTuple fileLastCommit = getRepositoryService().getFileLastCommitInfo(
                                                    repository, commit.getCommit().getId(), filePath);
        if(!blob.getIsLarge() && !blob.getIsBinary())
            codeHighLightService.process(filePath, blob);
        // TODO: надо не выводить файлы больше одного мегабайта которые весят
        PathBreadCrumbs pathBreadCrumb = PathBreadCrumbs.create(filePath);
        boolean isMarkdown = Objects.nonNull(plain) && plain.equals("1");
        if(FilenameUtils.getExtension(blob.getFullname()).equals("md") && Objects.isNull(plain)){
            try {
                String rawBody = IOUtils.toString(new InputStreamReader(blob.getInputStream()));
                String markdown = markdownService.process(
                    rawBody,
                    repository,
                    commit.getCommit(),
                    project.getAlias(),
                    alias.getValue(),
                    branch,
                    blob.getPath()
                );
                return new ModelAndView("blob/read_markdown")
                    .addObject("pCommit", commit)
                    .addObject("lastCommit",
                        Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                    .addObject("blob", blob)
                    .addObject("markdown", markdown)
                    .addObject("pathBreadCrumb", pathBreadCrumb)
                    .addObject("currentBranch", baseBranch);
            }catch (IOException e) {
                LOGGER.info(e.getMessage(), e);
                throw new DocumentNotFoundException();
            }
        }
        return new ModelAndView("blob/read")
                        .addObject("pCommit", commit)
                        .addObject("lastCommit",
                                Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                        .addObject("blob", blob)
                        .addObject("pathBreadCrumb", pathBreadCrumb)
                        .addObject("currentBranch", baseBranch)
                        .addObject("isMarkdown", isMarkdown);
    }
    @GetMapping("raw")
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ResponseEntity<InputStreamResource> rawFile(
        @RequestParam(value = "file") String filePath,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "inline", required = false, defaultValue = "true") boolean inline,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        BranchTuple baseBranch = getBranch(repository, project, null);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit %s", commitHash)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        String mimeType = URLConnection.guessContentTypeFromName(blob.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(blob.getSize());
        headers.setContentDisposition(
                ContentDisposition
                        .builder("inline")
                        .filename(blob.getName(), StandardCharsets.UTF_8)
                        .build()
        );
        if (!blob.getIsBinary() && !blob.getIsLarge() && inline)
            headers.setContentType(MediaType.TEXT_PLAIN);
        else if (Objects.nonNull(mimeType) && inline)
            headers.setContentType(MediaType.parseMediaType(mimeType));
        else
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        InputStreamResource inputStreamResource = new InputStreamResource(blob.getInputStream());
        return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
    }
}
@Controller
@DefaultExceptionHandler
@RequestMapping("project/{userAlias}/{projectAlias}/blob")
public class BlobController extends AbstractRepositoryController {
    private final CodeHighLightService codeHighLightService;
    private final SettingService settingService;
    private final MarkdownService markdownService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobController.class);
    @Autowired
    public BlobController(
        BranchService branchService,
        RepositoryService repositoryService,
        UserProjectAccessLevelService userProjectAccessLevelService,
        CodeHighLightService codeHighLightService,
        RepositorySecurityService repositorySecurityService,
        SettingService settingService, MarkdownService markdownService
    ) {
        super(branchService, repositoryService, repositorySecurityService,  userProjectAccessLevelService);
        this.codeHighLightService = codeHighLightService;
        this.settingService = settingService;
        this.markdownService = markdownService;
    }
    @GetMapping
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ModelAndView read(
        @RequestParam(value = "file", required = false) String filePath,
        @RequestParam(value = "branch", required = false) String branch,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "plain", required = false ) String plain,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        if (Objects.isNull(filePath) || filePath.isEmpty())
            return RedirectUtil.redirect(String.format("/project/%s/%s?branch=%s",
                                                    alias.getValue(),
                                                    project.getAlias(),
                                                    branch
                                                ));
        BranchTuple baseBranch = getBranch(repository, project, branch);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit for branch %s", branch)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        @Nullable CommitTuple fileLastCommit = getRepositoryService().getFileLastCommitInfo(
                                                    repository, commit.getCommit().getId(), filePath);
        if(!blob.getIsLarge() && !blob.getIsBinary())
            codeHighLightService.process(filePath, blob);
        // TODO: надо не выводить файлы больше одного мегабайта которые весят
        PathBreadCrumbs pathBreadCrumb = PathBreadCrumbs.create(filePath);
        boolean isMarkdown = Objects.nonNull(plain) && plain.equals("1");
        if(FilenameUtils.getExtension(blob.getFullname()).equals("md") && Objects.isNull(plain)){
            try {
                String rawBody = IOUtils.toString(new InputStreamReader(blob.getInputStream()));
                String markdown = markdownService.process(
                    rawBody,
                    repository,
                    commit.getCommit(),
                    project.getAlias(),
                    alias.getValue(),
                    branch,
                    blob.getPath()
                );
                return new ModelAndView("blob/read_markdown")
                    .addObject("pCommit", commit)
                    .addObject("lastCommit",
                        Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                    .addObject("blob", blob)
                    .addObject("markdown", markdown)
                    .addObject("pathBreadCrumb", pathBreadCrumb)
                    .addObject("currentBranch", baseBranch);
            }catch (IOException e) {
                LOGGER.info(e.getMessage(), e);
                throw new DocumentNotFoundException();
            }
        }
        return new ModelAndView("blob/read")
                        .addObject("pCommit", commit)
                        .addObject("lastCommit",
                                Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                        .addObject("blob", blob)
                        .addObject("pathBreadCrumb", pathBreadCrumb)
                        .addObject("currentBranch", baseBranch)
                        .addObject("isMarkdown", isMarkdown);
    }
    @GetMapping("raw")
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ResponseEntity<InputStreamResource> rawFile(
        @RequestParam(value = "file") String filePath,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "inline", required = false, defaultValue = "true") boolean inline,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        BranchTuple baseBranch = getBranch(repository, project, null);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit %s", commitHash)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        String mimeType = URLConnection.guessContentTypeFromName(blob.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(blob.getSize());
        headers.setContentDisposition(
                ContentDisposition
                        .builder("inline")
                        .filename(blob.getName(), StandardCharsets.UTF_8)
                        .build()
        );
        if (!blob.getIsBinary() && !blob.getIsLarge() && inline)
            headers.setContentType(MediaType.TEXT_PLAIN);
        else if (Objects.nonNull(mimeType) && inline)
            headers.setContentType(MediaType.parseMediaType(mimeType));
        else
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        InputStreamResource inputStreamResource = new InputStreamResource(blob.getInputStream());
        return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
    }
}
@Controller
@DefaultExceptionHandler
@RequestMapping("project/{userAlias}/{projectAlias}/blob")
public class BlobController extends AbstractRepositoryController {
    private final CodeHighLightService codeHighLightService;
    private final SettingService settingService;
    private final MarkdownService markdownService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobController.class);
    @Autowired
    public BlobController(
        BranchService branchService,
        RepositoryService repositoryService,
        UserProjectAccessLevelService userProjectAccessLevelService,
        CodeHighLightService codeHighLightService,
        RepositorySecurityService repositorySecurityService,
        SettingService settingService, MarkdownService markdownService
    ) {
        super(branchService, repositoryService, repositorySecurityService,  userProjectAccessLevelService);
        this.codeHighLightService = codeHighLightService;
        this.settingService = settingService;
        this.markdownService = markdownService;
    }
    @GetMapping
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ModelAndView read(
        @RequestParam(value = "file", required = false) String filePath,
        @RequestParam(value = "branch", required = false) String branch,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "plain", required = false ) String plain,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        if (Objects.isNull(filePath) || filePath.isEmpty())
            return RedirectUtil.redirect(String.format("/project/%s/%s?branch=%s",
                                                    alias.getValue(),
                                                    project.getAlias(),
                                                    branch
                                                ));
        BranchTuple baseBranch = getBranch(repository, project, branch);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit for branch %s", branch)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        @Nullable CommitTuple fileLastCommit = getRepositoryService().getFileLastCommitInfo(
                                                    repository, commit.getCommit().getId(), filePath);
        if(!blob.getIsLarge() && !blob.getIsBinary())
            codeHighLightService.process(filePath, blob);
        // TODO: надо не выводить файлы больше одного мегабайта которые весят
        PathBreadCrumbs pathBreadCrumb = PathBreadCrumbs.create(filePath);
        boolean isMarkdown = Objects.nonNull(plain) && plain.equals("1");
        if(FilenameUtils.getExtension(blob.getFullname()).equals("md") && Objects.isNull(plain)){
            try {
                String rawBody = IOUtils.toString(new InputStreamReader(blob.getInputStream()));
                String markdown = markdownService.process(
                    rawBody,
                    repository,
                    commit.getCommit(),
                    project.getAlias(),
                    alias.getValue(),
                    branch,
                    blob.getPath()
                );
                return new ModelAndView("blob/read_markdown")
                    .addObject("pCommit", commit)
                    .addObject("lastCommit",
                        Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                    .addObject("blob", blob)
                    .addObject("markdown", markdown)
                    .addObject("pathBreadCrumb", pathBreadCrumb)
                    .addObject("currentBranch", baseBranch);
            }catch (IOException e) {
                LOGGER.info(e.getMessage(), e);
                throw new DocumentNotFoundException();
            }
        }
        return new ModelAndView("blob/read")
                        .addObject("pCommit", commit)
                        .addObject("lastCommit",
                                Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                        .addObject("blob", blob)
                        .addObject("pathBreadCrumb", pathBreadCrumb)
                        .addObject("currentBranch", baseBranch)
                        .addObject("isMarkdown", isMarkdown);
    }
    @GetMapping("raw")
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ResponseEntity<InputStreamResource> rawFile(
        @RequestParam(value = "file") String filePath,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "inline", required = false, defaultValue = "true") boolean inline,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        BranchTuple baseBranch = getBranch(repository, project, null);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit %s", commitHash)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        String mimeType = URLConnection.guessContentTypeFromName(blob.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(blob.getSize());
        headers.setContentDisposition(
                ContentDisposition
                        .builder("inline")
                        .filename(blob.getName(), StandardCharsets.UTF_8)
                        .build()
        );
        if (!blob.getIsBinary() && !blob.getIsLarge() && inline)
            headers.setContentType(MediaType.TEXT_PLAIN);
        else if (Objects.nonNull(mimeType) && inline)
            headers.setContentType(MediaType.parseMediaType(mimeType));
        else
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        InputStreamResource inputStreamResource = new InputStreamResource(blob.getInputStream());
        return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
    }
}
@Controller
@DefaultExceptionHandler
@RequestMapping("project/{userAlias}/{projectAlias}/blob")
public class BlobController extends AbstractRepositoryController {
    private final CodeHighLightService codeHighLightService;
    private final SettingService settingService;
    private final MarkdownService markdownService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobController.class);
    @Autowired
    public BlobController(
        BranchService branchService,
        RepositoryService repositoryService,
        UserProjectAccessLevelService userProjectAccessLevelService,
        CodeHighLightService codeHighLightService,
        RepositorySecurityService repositorySecurityService,
        SettingService settingService, MarkdownService markdownService
    ) {
        super(branchService, repositoryService, repositorySecurityService,  userProjectAccessLevelService);
        this.codeHighLightService = codeHighLightService;
        this.settingService = settingService;
        this.markdownService = markdownService;
    }
    @GetMapping
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ModelAndView read(
        @RequestParam(value = "file", required = false) String filePath,
        @RequestParam(value = "branch", required = false) String branch,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "plain", required = false ) String plain,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        if (Objects.isNull(filePath) || filePath.isEmpty())
            return RedirectUtil.redirect(String.format("/project/%s/%s?branch=%s",
                                                    alias.getValue(),
                                                    project.getAlias(),
                                                    branch
                                                ));
        BranchTuple baseBranch = getBranch(repository, project, branch);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit for branch %s", branch)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        @Nullable CommitTuple fileLastCommit = getRepositoryService().getFileLastCommitInfo(
                                                    repository, commit.getCommit().getId(), filePath);
        if(!blob.getIsLarge() && !blob.getIsBinary())
            codeHighLightService.process(filePath, blob);
        // TODO: надо не выводить файлы больше одного мегабайта которые весят
        PathBreadCrumbs pathBreadCrumb = PathBreadCrumbs.create(filePath);
        boolean isMarkdown = Objects.nonNull(plain) && plain.equals("1");
        if(FilenameUtils.getExtension(blob.getFullname()).equals("md") && Objects.isNull(plain)){
            try {
                String rawBody = IOUtils.toString(new InputStreamReader(blob.getInputStream()));
                String markdown = markdownService.process(
                    rawBody,
                    repository,
                    commit.getCommit(),
                    project.getAlias(),
                    alias.getValue(),
                    branch,
                    blob.getPath()
                );
                return new ModelAndView("blob/read_markdown")
                    .addObject("pCommit", commit)
                    .addObject("lastCommit",
                        Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                    .addObject("blob", blob)
                    .addObject("markdown", markdown)
                    .addObject("pathBreadCrumb", pathBreadCrumb)
                    .addObject("currentBranch", baseBranch);
            }catch (IOException e) {
                LOGGER.info(e.getMessage(), e);
                throw new DocumentNotFoundException();
            }
        }
        return new ModelAndView("blob/read")
                        .addObject("pCommit", commit)
                        .addObject("lastCommit",
                                Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                        .addObject("blob", blob)
                        .addObject("pathBreadCrumb", pathBreadCrumb)
                        .addObject("currentBranch", baseBranch)
                        .addObject("isMarkdown", isMarkdown);
    }
    @GetMapping("raw")
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ResponseEntity<InputStreamResource> rawFile(
        @RequestParam(value = "file") String filePath,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "inline", required = false, defaultValue = "true") boolean inline,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        BranchTuple baseBranch = getBranch(repository, project, null);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit %s", commitHash)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        String mimeType = URLConnection.guessContentTypeFromName(blob.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(blob.getSize());
        headers.setContentDisposition(
                ContentDisposition
                        .builder("inline")
                        .filename(blob.getName(), StandardCharsets.UTF_8)
                        .build()
        );
        if (!blob.getIsBinary() && !blob.getIsLarge() && inline)
            headers.setContentType(MediaType.TEXT_PLAIN);
        else if (Objects.nonNull(mimeType) && inline)
            headers.setContentType(MediaType.parseMediaType(mimeType));
        else
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        InputStreamResource inputStreamResource = new InputStreamResource(blob.getInputStream());
        return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
    }
}
@Controller
@DefaultExceptionHandler
@RequestMapping("project/{userAlias}/{projectAlias}/blob")
public class BlobController extends AbstractRepositoryController {
    private final CodeHighLightService codeHighLightService;
    private final SettingService settingService;
    private final MarkdownService markdownService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobController.class);
    @Autowired
    public BlobController(
        BranchService branchService,
        RepositoryService repositoryService,
        UserProjectAccessLevelService userProjectAccessLevelService,
        CodeHighLightService codeHighLightService,
        RepositorySecurityService repositorySecurityService,
        SettingService settingService, MarkdownService markdownService
    ) {
        super(branchService, repositoryService, repositorySecurityService,  userProjectAccessLevelService);
        this.codeHighLightService = codeHighLightService;
        this.settingService = settingService;
        this.markdownService = markdownService;
    }
    @GetMapping
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ModelAndView read(
        @RequestParam(value = "file", required = false) String filePath,
        @RequestParam(value = "branch", required = false) String branch,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "plain", required = false ) String plain,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        if (Objects.isNull(filePath) || filePath.isEmpty())
            return RedirectUtil.redirect(String.format("/project/%s/%s?branch=%s",
                                                    alias.getValue(),
                                                    project.getAlias(),
                                                    branch
                                                ));
        BranchTuple baseBranch = getBranch(repository, project, branch);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit for branch %s", branch)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        @Nullable CommitTuple fileLastCommit = getRepositoryService().getFileLastCommitInfo(
                                                    repository, commit.getCommit().getId(), filePath);
        if(!blob.getIsLarge() && !blob.getIsBinary())
            codeHighLightService.process(filePath, blob);
        // TODO: надо не выводить файлы больше одного мегабайта которые весят
        PathBreadCrumbs pathBreadCrumb = PathBreadCrumbs.create(filePath);
        boolean isMarkdown = Objects.nonNull(plain) && plain.equals("1");
        if(FilenameUtils.getExtension(blob.getFullname()).equals("md") && Objects.isNull(plain)){
            try {
                String rawBody = IOUtils.toString(new InputStreamReader(blob.getInputStream()));
                String markdown = markdownService.process(
                    rawBody,
                    repository,
                    commit.getCommit(),
                    project.getAlias(),
                    alias.getValue(),
                    branch,
                    blob.getPath()
                );
                return new ModelAndView("blob/read_markdown")
                    .addObject("pCommit", commit)
                    .addObject("lastCommit",
                        Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                    .addObject("blob", blob)
                    .addObject("markdown", markdown)
                    .addObject("pathBreadCrumb", pathBreadCrumb)
                    .addObject("currentBranch", baseBranch);
            }catch (IOException e) {
                LOGGER.info(e.getMessage(), e);
                throw new DocumentNotFoundException();
            }
        }
        return new ModelAndView("blob/read")
                        .addObject("pCommit", commit)
                        .addObject("lastCommit",
                                Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                        .addObject("blob", blob)
                        .addObject("pathBreadCrumb", pathBreadCrumb)
                        .addObject("currentBranch", baseBranch)
                        .addObject("isMarkdown", isMarkdown);
    }
    @GetMapping("raw")
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ResponseEntity<InputStreamResource> rawFile(
        @RequestParam(value = "file") String filePath,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "inline", required = false, defaultValue = "true") boolean inline,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        BranchTuple baseBranch = getBranch(repository, project, null);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit %s", commitHash)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        String mimeType = URLConnection.guessContentTypeFromName(blob.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(blob.getSize());
        headers.setContentDisposition(
                ContentDisposition
                        .builder("inline")
                        .filename(blob.getName(), StandardCharsets.UTF_8)
                        .build()
        );
        if (!blob.getIsBinary() && !blob.getIsLarge() && inline)
            headers.setContentType(MediaType.TEXT_PLAIN);
        else if (Objects.nonNull(mimeType) && inline)
            headers.setContentType(MediaType.parseMediaType(mimeType));
        else
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        InputStreamResource inputStreamResource = new InputStreamResource(blob.getInputStream());
        return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
    }
}
@Controller
@DefaultExceptionHandler
@RequestMapping("project/{userAlias}/{projectAlias}/blob")
public class BlobController extends AbstractRepositoryController {
    private final CodeHighLightService codeHighLightService;
    private final SettingService settingService;
    private final MarkdownService markdownService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobController.class);
    @Autowired
    public BlobController(
        BranchService branchService,
        RepositoryService repositoryService,
        UserProjectAccessLevelService userProjectAccessLevelService,
        CodeHighLightService codeHighLightService,
        RepositorySecurityService repositorySecurityService,
        SettingService settingService, MarkdownService markdownService
    ) {
        super(branchService, repositoryService, repositorySecurityService,  userProjectAccessLevelService);
        this.codeHighLightService = codeHighLightService;
        this.settingService = settingService;
        this.markdownService = markdownService;
    }
    @GetMapping
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ModelAndView read(
        @RequestParam(value = "file", required = false) String filePath,
        @RequestParam(value = "branch", required = false) String branch,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "plain", required = false ) String plain,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        if (Objects.isNull(filePath) || filePath.isEmpty())
            return RedirectUtil.redirect(String.format("/project/%s/%s?branch=%s",
                                                    alias.getValue(),
                                                    project.getAlias(),
                                                    branch
                                                ));
        BranchTuple baseBranch = getBranch(repository, project, branch);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit for branch %s", branch)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        @Nullable CommitTuple fileLastCommit = getRepositoryService().getFileLastCommitInfo(
                                                    repository, commit.getCommit().getId(), filePath);
        if(!blob.getIsLarge() && !blob.getIsBinary())
            codeHighLightService.process(filePath, blob);
        // TODO: надо не выводить файлы больше одного мегабайта которые весят
        PathBreadCrumbs pathBreadCrumb = PathBreadCrumbs.create(filePath);
        boolean isMarkdown = Objects.nonNull(plain) && plain.equals("1");
        if(FilenameUtils.getExtension(blob.getFullname()).equals("md") && Objects.isNull(plain)){
            try {
                String rawBody = IOUtils.toString(new InputStreamReader(blob.getInputStream()));
                String markdown = markdownService.process(
                    rawBody,
                    repository,
                    commit.getCommit(),
                    project.getAlias(),
                    alias.getValue(),
                    branch,
                    blob.getPath()
                );
                return new ModelAndView("blob/read_markdown")
                    .addObject("pCommit", commit)
                    .addObject("lastCommit",
                        Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                    .addObject("blob", blob)
                    .addObject("markdown", markdown)
                    .addObject("pathBreadCrumb", pathBreadCrumb)
                    .addObject("currentBranch", baseBranch);
            }catch (IOException e) {
                LOGGER.info(e.getMessage(), e);
                throw new DocumentNotFoundException();
            }
        }
        return new ModelAndView("blob/read")
                        .addObject("pCommit", commit)
                        .addObject("lastCommit",
                                Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                        .addObject("blob", blob)
                        .addObject("pathBreadCrumb", pathBreadCrumb)
                        .addObject("currentBranch", baseBranch)
                        .addObject("isMarkdown", isMarkdown);
    }
    @GetMapping("raw")
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ResponseEntity<InputStreamResource> rawFile(
        @RequestParam(value = "file") String filePath,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "inline", required = false, defaultValue = "true") boolean inline,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        BranchTuple baseBranch = getBranch(repository, project, null);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit %s", commitHash)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        String mimeType = URLConnection.guessContentTypeFromName(blob.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(blob.getSize());
        headers.setContentDisposition(
                ContentDisposition
                        .builder("inline")
                        .filename(blob.getName(), StandardCharsets.UTF_8)
                        .build()
        );
        if (!blob.getIsBinary() && !blob.getIsLarge() && inline)
            headers.setContentType(MediaType.TEXT_PLAIN);
        else if (Objects.nonNull(mimeType) && inline)
            headers.setContentType(MediaType.parseMediaType(mimeType));
        else
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        InputStreamResource inputStreamResource = new InputStreamResource(blob.getInputStream());
        return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
    }
}
@Controller
@DefaultExceptionHandler
@RequestMapping("project/{userAlias}/{projectAlias}/blob")
public class BlobController extends AbstractRepositoryController {
    private final CodeHighLightService codeHighLightService;
    private final SettingService settingService;
    private final MarkdownService markdownService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobController.class);
    @Autowired
    public BlobController(
        BranchService branchService,
        RepositoryService repositoryService,
        UserProjectAccessLevelService userProjectAccessLevelService,
        CodeHighLightService codeHighLightService,
        RepositorySecurityService repositorySecurityService,
        SettingService settingService, MarkdownService markdownService
    ) {
        super(branchService, repositoryService, repositorySecurityService,  userProjectAccessLevelService);
        this.codeHighLightService = codeHighLightService;
        this.settingService = settingService;
        this.markdownService = markdownService;
    }
    @GetMapping
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ModelAndView read(
        @RequestParam(value = "file", required = false) String filePath,
        @RequestParam(value = "branch", required = false) String branch,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "plain", required = false ) String plain,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        if (Objects.isNull(filePath) || filePath.isEmpty())
            return RedirectUtil.redirect(String.format("/project/%s/%s?branch=%s",
                                                    alias.getValue(),
                                                    project.getAlias(),
                                                    branch
                                                ));
        BranchTuple baseBranch = getBranch(repository, project, branch);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit for branch %s", branch)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        @Nullable CommitTuple fileLastCommit = getRepositoryService().getFileLastCommitInfo(
                                                    repository, commit.getCommit().getId(), filePath);
        if(!blob.getIsLarge() && !blob.getIsBinary())
            codeHighLightService.process(filePath, blob);
        // TODO: Ð½Ð°Ð´Ð¾ Ð½Ðµ Ð²Ñ‹Ð²Ð¾Ð´Ð¸Ñ‚ÑŒ Ñ„Ð°Ð¹Ð»Ñ‹ Ð±Ð¾Ð»ÑŒÑˆÐµ Ð¾Ð´Ð½Ð¾Ð³Ð¾ Ð¼ÐµÐ³Ð°Ð±Ð°Ð¹Ñ‚Ð° ÐºÐ¾Ñ‚Ð¾Ñ€Ñ‹Ðµ Ð²ÐµÑÑÑ‚
        PathBreadCrumbs pathBreadCrumb = PathBreadCrumbs.create(filePath);
        boolean isMarkdown = Objects.nonNull(plain) && plain.equals("1");
        if(FilenameUtils.getExtension(blob.getFullname()).equals("md") && Objects.isNull(plain)){
            try {
                String rawBody = IOUtils.toString(new InputStreamReader(blob.getInputStream()));
                String markdown = markdownService.process(
                    rawBody,
                    repository,
                    commit.getCommit(),
                    project.getAlias(),
                    alias.getValue(),
                    branch,
                    blob.getPath()
                );
                return new ModelAndView("blob/read_markdown")
                    .addObject("pCommit", commit)
                    .addObject("lastCommit",
                        Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                    .addObject("blob", blob)
                    .addObject("markdown", markdown)
                    .addObject("pathBreadCrumb", pathBreadCrumb)
                    .addObject("currentBranch", baseBranch);
            }catch (IOException e) {
                LOGGER.info(e.getMessage(), e);
                throw new DocumentNotFoundException();
            }
        }
        return new ModelAndView("blob/read")
                        .addObject("pCommit", commit)
                        .addObject("lastCommit",
                                Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                        .addObject("blob", blob)
                        .addObject("pathBreadCrumb", pathBreadCrumb)
                        .addObject("currentBranch", baseBranch)
                        .addObject("isMarkdown", isMarkdown);
    }
    @GetMapping("raw")
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ResponseEntity<InputStreamResource> rawFile(
        @RequestParam(value = "file") String filePath,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "inline", required = false, defaultValue = "true") boolean inline,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        BranchTuple baseBranch = getBranch(repository, project, null);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit %s", commitHash)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        String mimeType = URLConnection.guessContentTypeFromName(blob.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(blob.getSize());
        headers.setContentDisposition(
                ContentDisposition
                        .builder("inline")
                        .filename(blob.getName(), StandardCharsets.UTF_8)
                        .build()
        );
        if (!blob.getIsBinary() && !blob.getIsLarge() && inline)
            headers.setContentType(MediaType.TEXT_PLAIN);
        else if (Objects.nonNull(mimeType) && inline)
            headers.setContentType(MediaType.parseMediaType(mimeType));
        else
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        InputStreamResource inputStreamResource = new InputStreamResource(blob.getInputStream());
        return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
    }
}
@Controller
@DefaultExceptionHandler
@RequestMapping("project/{userAlias}/{projectAlias}/blob")
public class BlobController extends AbstractRepositoryController {
    private final CodeHighLightService codeHighLightService;
    private final SettingService settingService;
    private final MarkdownService markdownService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobController.class);
    @Autowired
    public BlobController(
        BranchService branchService,
        RepositoryService repositoryService,
        UserProjectAccessLevelService userProjectAccessLevelService,
        CodeHighLightService codeHighLightService,
        RepositorySecurityService repositorySecurityService,
        SettingService settingService, MarkdownService markdownService
    ) {
        super(branchService, repositoryService, repositorySecurityService,  userProjectAccessLevelService);
        this.codeHighLightService = codeHighLightService;
        this.settingService = settingService;
        this.markdownService = markdownService;
    }
    @GetMapping
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ModelAndView read(
        @RequestParam(value = "file", required = false) String filePath,
        @RequestParam(value = "branch", required = false) String branch,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "plain", required = false ) String plain,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        if (Objects.isNull(filePath) || filePath.isEmpty())
            return RedirectUtil.redirect(String.format("/project/%s/%s?branch=%s",
                                                    alias.getValue(),
                                                    project.getAlias(),
                                                    branch
                                                ));
        BranchTuple baseBranch = getBranch(repository, project, branch);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit for branch %s", branch)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        @Nullable CommitTuple fileLastCommit = getRepositoryService().getFileLastCommitInfo(
                                                    repository, commit.getCommit().getId(), filePath);
        if(!blob.getIsLarge() && !blob.getIsBinary())
            codeHighLightService.process(filePath, blob);
        // TODO: Ð½Ð°Ð´Ð¾ Ð½Ðµ Ð²Ñ‹Ð²Ð¾Ð´Ð¸Ñ‚ÑŒ Ñ„Ð°Ð¹Ð»Ñ‹ Ð±Ð¾Ð»ÑŒÑˆÐµ Ð¾Ð´Ð½Ð¾Ð³Ð¾ Ð¼ÐµÐ³Ð°Ð±Ð°Ð¹Ñ‚Ð° ÐºÐ¾Ñ‚Ð¾Ñ€Ñ‹Ðµ Ð²ÐµÑÑÑ‚
        PathBreadCrumbs pathBreadCrumb = PathBreadCrumbs.create(filePath);
        boolean isMarkdown = Objects.nonNull(plain) && plain.equals("1");
        if(FilenameUtils.getExtension(blob.getFullname()).equals("md") && Objects.isNull(plain)){
            try {
                String rawBody = IOUtils.toString(new InputStreamReader(blob.getInputStream()));
                String markdown = markdownService.process(
                    rawBody,
                    repository,
                    commit.getCommit(),
                    project.getAlias(),
                    alias.getValue(),
                    branch,
                    blob.getPath()
                );
                return new ModelAndView("blob/read_markdown")
                    .addObject("pCommit", commit)
                    .addObject("lastCommit",
                        Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                    .addObject("blob", blob)
                    .addObject("markdown", markdown)
                    .addObject("pathBreadCrumb", pathBreadCrumb)
                    .addObject("currentBranch", baseBranch);
            }catch (IOException e) {
                LOGGER.info(e.getMessage(), e);
                throw new DocumentNotFoundException();
            }
        }
        return new ModelAndView("blob/read")
                        .addObject("pCommit", commit)
                        .addObject("lastCommit",
                                Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                        .addObject("blob", blob)
                        .addObject("pathBreadCrumb", pathBreadCrumb)
                        .addObject("currentBranch", baseBranch)
                        .addObject("isMarkdown", isMarkdown);
    }
    @GetMapping("raw")
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ResponseEntity<InputStreamResource> rawFile(
        @RequestParam(value = "file") String filePath,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "inline", required = false, defaultValue = "true") boolean inline,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        BranchTuple baseBranch = getBranch(repository, project, null);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit %s", commitHash)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        String mimeType = URLConnection.guessContentTypeFromName(blob.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(blob.getSize());
        headers.setContentDisposition(
                ContentDisposition
                        .builder("inline")
                        .filename(blob.getName(), StandardCharsets.UTF_8)
                        .build()
        );
        if (!blob.getIsBinary() && !blob.getIsLarge() && inline)
            headers.setContentType(MediaType.TEXT_PLAIN);
        else if (Objects.nonNull(mimeType) && inline)
            headers.setContentType(MediaType.parseMediaType(mimeType));
        else
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        InputStreamResource inputStreamResource = new InputStreamResource(blob.getInputStream());
        return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
    }
}
@Controller
@DefaultExceptionHandler
@RequestMapping("project/{userAlias}/{projectAlias}/blob")
public class BlobController extends AbstractRepositoryController {
    private final CodeHighLightService codeHighLightService;
    private final SettingService settingService;
    private final MarkdownService markdownService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobController.class);
    @Autowired
    public BlobController(
        BranchService branchService,
        RepositoryService repositoryService,
        UserProjectAccessLevelService userProjectAccessLevelService,
        CodeHighLightService codeHighLightService,
        RepositorySecurityService repositorySecurityService,
        SettingService settingService, MarkdownService markdownService
    ) {
        super(branchService, repositoryService, repositorySecurityService,  userProjectAccessLevelService);
        this.codeHighLightService = codeHighLightService;
        this.settingService = settingService;
        this.markdownService = markdownService;
    }
    @GetMapping
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ModelAndView read(
        @RequestParam(value = "file", required = false) String filePath,
        @RequestParam(value = "branch", required = false) String branch,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "plain", required = false ) String plain,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        if (Objects.isNull(filePath) || filePath.isEmpty())
            return RedirectUtil.redirect(String.format("/project/%s/%s?branch=%s",
                                                    alias.getValue(),
                                                    project.getAlias(),
                                                    branch
                                                ));
        BranchTuple baseBranch = getBranch(repository, project, branch);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit for branch %s", branch)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        @Nullable CommitTuple fileLastCommit = getRepositoryService().getFileLastCommitInfo(
                                                    repository, commit.getCommit().getId(), filePath);
        if(!blob.getIsLarge() && !blob.getIsBinary())
            codeHighLightService.process(filePath, blob);
        // TODO: Ð½Ð°Ð´Ð¾ Ð½Ðµ Ð²Ñ‹Ð²Ð¾Ð´Ð¸Ñ‚ÑŒ Ñ„Ð°Ð¹Ð»Ñ‹ Ð±Ð¾Ð»ÑŒÑˆÐµ Ð¾Ð´Ð½Ð¾Ð³Ð¾ Ð¼ÐµÐ³Ð°Ð±Ð°Ð¹Ñ‚Ð° ÐºÐ¾Ñ‚Ð¾Ñ€Ñ‹Ðµ Ð²ÐµÑÑÑ‚
        PathBreadCrumbs pathBreadCrumb = PathBreadCrumbs.create(filePath);
        boolean isMarkdown = Objects.nonNull(plain) && plain.equals("1");
        if(FilenameUtils.getExtension(blob.getFullname()).equals("md") && Objects.isNull(plain)){
            try {
                String rawBody = IOUtils.toString(new InputStreamReader(blob.getInputStream()));
                String markdown = markdownService.process(
                    rawBody,
                    repository,
                    commit.getCommit(),
                    project.getAlias(),
                    alias.getValue(),
                    branch,
                    blob.getPath()
                );
                return new ModelAndView("blob/read_markdown")
                    .addObject("pCommit", commit)
                    .addObject("lastCommit",
                        Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                    .addObject("blob", blob)
                    .addObject("markdown", markdown)
                    .addObject("pathBreadCrumb", pathBreadCrumb)
                    .addObject("currentBranch", baseBranch);
            }catch (IOException e) {
                LOGGER.info(e.getMessage(), e);
                throw new DocumentNotFoundException();
            }
        }
        return new ModelAndView("blob/read")
                        .addObject("pCommit", commit)
                        .addObject("lastCommit",
                                Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                        .addObject("blob", blob)
                        .addObject("pathBreadCrumb", pathBreadCrumb)
                        .addObject("currentBranch", baseBranch)
                        .addObject("isMarkdown", isMarkdown);
    }
    @GetMapping("raw")
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ResponseEntity<InputStreamResource> rawFile(
        @RequestParam(value = "file") String filePath,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "inline", required = false, defaultValue = "true") boolean inline,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        BranchTuple baseBranch = getBranch(repository, project, null);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit %s", commitHash)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        String mimeType = URLConnection.guessContentTypeFromName(blob.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(blob.getSize());
        headers.setContentDisposition(
                ContentDisposition
                        .builder("inline")
                        .filename(blob.getName(), StandardCharsets.UTF_8)
                        .build()
        );
        if (!blob.getIsBinary() && !blob.getIsLarge() && inline)
            headers.setContentType(MediaType.TEXT_PLAIN);
        else if (Objects.nonNull(mimeType) && inline)
            headers.setContentType(MediaType.parseMediaType(mimeType));
        else
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        InputStreamResource inputStreamResource = new InputStreamResource(blob.getInputStream());
        return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
    }
}
@Controller
@DefaultExceptionHandler
@RequestMapping("project/{userAlias}/{projectAlias}/blob")
public class BlobController extends AbstractRepositoryController {
    private final CodeHighLightService codeHighLightService;
    private final SettingService settingService;
    private final MarkdownService markdownService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobController.class);
    @Autowired
    public BlobController(
        BranchService branchService,
        RepositoryService repositoryService,
        UserProjectAccessLevelService userProjectAccessLevelService,
        CodeHighLightService codeHighLightService,
        RepositorySecurityService repositorySecurityService,
        SettingService settingService, MarkdownService markdownService
    ) {
        super(branchService, repositoryService, repositorySecurityService,  userProjectAccessLevelService);
        this.codeHighLightService = codeHighLightService;
        this.settingService = settingService;
        this.markdownService = markdownService;
    }
    @GetMapping
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ModelAndView read(
        @RequestParam(value = "file", required = false) String filePath,
        @RequestParam(value = "branch", required = false) String branch,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "plain", required = false ) String plain,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        if (Objects.isNull(filePath) || filePath.isEmpty())
            return RedirectUtil.redirect(String.format("/project/%s/%s?branch=%s",
                                                    alias.getValue(),
                                                    project.getAlias(),
                                                    branch
                                                ));
        BranchTuple baseBranch = getBranch(repository, project, branch);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit for branch %s", branch)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        @Nullable CommitTuple fileLastCommit = getRepositoryService().getFileLastCommitInfo(
                                                    repository, commit.getCommit().getId(), filePath);
        if(!blob.getIsLarge() && !blob.getIsBinary())
            codeHighLightService.process(filePath, blob);
        // TODO: Ð½Ð°Ð´Ð¾ Ð½Ðµ Ð²Ñ‹Ð²Ð¾Ð´Ð¸Ñ‚ÑŒ Ñ„Ð°Ð¹Ð»Ñ‹ Ð±Ð¾Ð»ÑŒÑˆÐµ Ð¾Ð´Ð½Ð¾Ð³Ð¾ Ð¼ÐµÐ³Ð°Ð±Ð°Ð¹Ñ‚Ð° ÐºÐ¾Ñ‚Ð¾Ñ€Ñ‹Ðµ Ð²ÐµÑÑÑ‚
        PathBreadCrumbs pathBreadCrumb = PathBreadCrumbs.create(filePath);
        boolean isMarkdown = Objects.nonNull(plain) && plain.equals("1");
        if(FilenameUtils.getExtension(blob.getFullname()).equals("md") && Objects.isNull(plain)){
            try {
                String rawBody = IOUtils.toString(new InputStreamReader(blob.getInputStream()));
                String markdown = markdownService.process(
                    rawBody,
                    repository,
                    commit.getCommit(),
                    project.getAlias(),
                    alias.getValue(),
                    branch,
                    blob.getPath()
                );
                return new ModelAndView("blob/read_markdown")
                    .addObject("pCommit", commit)
                    .addObject("lastCommit",
                        Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                    .addObject("blob", blob)
                    .addObject("markdown", markdown)
                    .addObject("pathBreadCrumb", pathBreadCrumb)
                    .addObject("currentBranch", baseBranch);
            }catch (IOException e) {
                LOGGER.info(e.getMessage(), e);
                throw new DocumentNotFoundException();
            }
        }
        return new ModelAndView("blob/read")
                        .addObject("pCommit", commit)
                        .addObject("lastCommit",
                                Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                        .addObject("blob", blob)
                        .addObject("pathBreadCrumb", pathBreadCrumb)
                        .addObject("currentBranch", baseBranch)
                        .addObject("isMarkdown", isMarkdown);
    }
    @GetMapping("raw")
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ResponseEntity<InputStreamResource> rawFile(
        @RequestParam(value = "file") String filePath,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "inline", required = false, defaultValue = "true") boolean inline,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        BranchTuple baseBranch = getBranch(repository, project, null);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit %s", commitHash)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        String mimeType = URLConnection.guessContentTypeFromName(blob.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(blob.getSize());
        headers.setContentDisposition(
                ContentDisposition
                        .builder("inline")
                        .filename(blob.getName(), StandardCharsets.UTF_8)
                        .build()
        );
        if (!blob.getIsBinary() && !blob.getIsLarge() && inline)
            headers.setContentType(MediaType.TEXT_PLAIN);
        else if (Objects.nonNull(mimeType) && inline)
            headers.setContentType(MediaType.parseMediaType(mimeType));
        else
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        InputStreamResource inputStreamResource = new InputStreamResource(blob.getInputStream());
        return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
    }
}
@Controller
@DefaultExceptionHandler
@RequestMapping("project/{userAlias}/{projectAlias}/blob")
public class BlobController extends AbstractRepositoryController {
    private final CodeHighLightService codeHighLightService;
    private final SettingService settingService;
    private final MarkdownService markdownService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobController.class);
    @Autowired
    public BlobController(
        BranchService branchService,
        RepositoryService repositoryService,
        UserProjectAccessLevelService userProjectAccessLevelService,
        CodeHighLightService codeHighLightService,
        RepositorySecurityService repositorySecurityService,
        SettingService settingService, MarkdownService markdownService
    ) {
        super(branchService, repositoryService, repositorySecurityService,  userProjectAccessLevelService);
        this.codeHighLightService = codeHighLightService;
        this.settingService = settingService;
        this.markdownService = markdownService;
    }
    @GetMapping
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ModelAndView read(
        @RequestParam(value = "file", required = false) String filePath,
        @RequestParam(value = "branch", required = false) String branch,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "plain", required = false ) String plain,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        if (Objects.isNull(filePath) || filePath.isEmpty())
            return RedirectUtil.redirect(String.format("/project/%s/%s?branch=%s",
                                                    alias.getValue(),
                                                    project.getAlias(),
                                                    branch
                                                ));
        BranchTuple baseBranch = getBranch(repository, project, branch);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit for branch %s", branch)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        @Nullable CommitTuple fileLastCommit = getRepositoryService().getFileLastCommitInfo(
                                                    repository, commit.getCommit().getId(), filePath);
        if(!blob.getIsLarge() && !blob.getIsBinary())
            codeHighLightService.process(filePath, blob);
        // TODO: Ð½Ð°Ð´Ð¾ Ð½Ðµ Ð²Ñ‹Ð²Ð¾Ð´Ð¸Ñ‚ÑŒ Ñ„Ð°Ð¹Ð»Ñ‹ Ð±Ð¾Ð»ÑŒÑˆÐµ Ð¾Ð´Ð½Ð¾Ð³Ð¾ Ð¼ÐµÐ³Ð°Ð±Ð°Ð¹Ñ‚Ð° ÐºÐ¾Ñ‚Ð¾Ñ€Ñ‹Ðµ Ð²ÐµÑÑÑ‚
        PathBreadCrumbs pathBreadCrumb = PathBreadCrumbs.create(filePath);
        boolean isMarkdown = Objects.nonNull(plain) && plain.equals("1");
        if(FilenameUtils.getExtension(blob.getFullname()).equals("md") && Objects.isNull(plain)){
            try {
                String rawBody = IOUtils.toString(new InputStreamReader(blob.getInputStream()));
                String markdown = markdownService.process(
                    rawBody,
                    repository,
                    commit.getCommit(),
                    project.getAlias(),
                    alias.getValue(),
                    branch,
                    blob.getPath()
                );
                return new ModelAndView("blob/read_markdown")
                    .addObject("pCommit", commit)
                    .addObject("lastCommit",
                        Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                    .addObject("blob", blob)
                    .addObject("markdown", markdown)
                    .addObject("pathBreadCrumb", pathBreadCrumb)
                    .addObject("currentBranch", baseBranch);
            }catch (IOException e) {
                LOGGER.info(e.getMessage(), e);
                throw new DocumentNotFoundException();
            }
        }
        return new ModelAndView("blob/read")
                        .addObject("pCommit", commit)
                        .addObject("lastCommit",
                                Objects.nonNull(fileLastCommit) ? fileLastCommit : commit)
                        .addObject("blob", blob)
                        .addObject("pathBreadCrumb", pathBreadCrumb)
                        .addObject("currentBranch", baseBranch)
                        .addObject("isMarkdown", isMarkdown);
    }
    @GetMapping("raw")
    @PreAuthorize("@repositorySecurityService.hasAccess(#alias, #project, principal, 'GUEST')")
    public ResponseEntity<InputStreamResource> rawFile(
        @RequestParam(value = "file") String filePath,
        @RequestParam(value = "commit", required = false) String commitHash,
        @RequestParam(value = "inline", required = false, defaultValue = "true") boolean inline,
        AliasModel alias,
        ProjectModel project,
        Repository repository
    ) {
        BranchTuple baseBranch = getBranch(repository, project, null);
        @Nullable CommitTuple commit = Objects.isNull(commitHash)
                                            ? getRepositoryService().wrapCommitTuple(baseBranch.getLastCommit())
                                            : getRepositoryService().getCommitByIdInfo(repository, commitHash);
        if(Objects.isNull(commit))
            throw new CommitNotFoundException(
                String.format("Can't find commit %s", commitHash)
            );
        @Nullable BlobTuple blob = getRepositoryService().getBlobContent(repository, commit.getCommit(), filePath);
        if (Objects.isNull(blob))
            throw new BlobNotFoundException();
        String mimeType = URLConnection.guessContentTypeFromName(blob.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(blob.getSize());
        headers.setContentDisposition(
                ContentDisposition
                        .builder("inline")
                        .filename(blob.getName(), StandardCharsets.UTF_8)
                        .build()
        );
        if (!blob.getIsBinary() && !blob.getIsLarge() && inline)
            headers.setContentType(MediaType.TEXT_PLAIN);
        else if (Objects.nonNull(mimeType) && inline)
            headers.setContentType(MediaType.parseMediaType(mimeType));
        else
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        InputStreamResource inputStreamResource = new InputStreamResource(blob.getInputStream());
        return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
    }
}
